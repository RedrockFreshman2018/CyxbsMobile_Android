package com.mredrock.cyxbs.ui.activity.qa

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.mredrock.cyxbs.BaseAPP
import com.mredrock.cyxbs.R
import com.mredrock.cyxbs.component.multi_image_selector.MultiImageSelectorActivity
import com.mredrock.cyxbs.model.qa.Draft
import com.mredrock.cyxbs.model.social.Image
import com.mredrock.cyxbs.network.RequestManager
import com.mredrock.cyxbs.network.error.QAErrorHandler
import com.mredrock.cyxbs.subscriber.SimpleObserver
import com.mredrock.cyxbs.subscriber.SubscriberListener
import com.mredrock.cyxbs.ui.activity.BaseActivity
import com.mredrock.cyxbs.util.extensions.doPermissionAction
import kotlinx.android.synthetic.main.activity_answer_question.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.File

class AnswerQuestionActivity : BaseActivity() {
    private lateinit var qid: String
    private var saveCache = true
    private var maxLength: Int = 0
    private var imageList: MutableList<Image> = mutableListOf(Image(ADD_IMAGE_PATH, Image.TYPE_ADD))
    private var draftId: String? = null

    companion object {
        @JvmStatic
        private var cache = ""

        private const val ADD_IMAGE_PATH = "file:///android_asset/add_news.jpg"
        private const val MAX_IMAGE_NUM = 4
        private const val SELECT_IMAGE = 0x123
        const val ANSWER = 0x12

        @JvmStatic
        fun start(context: Activity, questionId: String) {
            context.startActivityForResult<AnswerQuestionActivity>(ANSWER, "questionId" to questionId)
        }

        @JvmStatic
        fun startFromDraft(context: Activity, questionId: String, draftId: String, text: String) {
            context.startActivityForResult<AnswerQuestionActivity>(ANSWER,
                    "questionId" to questionId,
                    "draftId" to draftId,
                    "text" to text
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_question)
        initData()
        initView()
    }

    private fun initData() {
        qid = intent.getStringExtra("questionId")
        cache = intent.getStringExtra("text") ?: ""
        draftId = intent.getStringExtra("draftId")
        maxLength = resources.getInteger(R.integer.answer_max_length)
    }

    private fun initView() {
        toolbar.setLeftTextListener { finish() }
        toolbar.setRightTextListener { upload() }
        counter.text = "${maxLength - cache.length}"
        content.setText(cache)
        content.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                counter.text = "${maxLength - s.length}"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        images.setImagesData(imageList)
        images.setOnAddImagItemClickListener { _, _ ->
            doPermissionAction(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    action = {
                        val selected = if (imageList.size > 1)
                            MutableList(imageList.size - 1) { imageList[it + 1].url }
                        else mutableListOf<String>()

                        startActivityForResult<MultiImageSelectorActivity>(SELECT_IMAGE,
                                MultiImageSelectorActivity.EXTRA_SHOW_CAMERA to true,
                                MultiImageSelectorActivity.EXTRA_SELECT_COUNT to MAX_IMAGE_NUM,
                                MultiImageSelectorActivity.EXTRA_SELECT_MODE to MultiImageSelectorActivity.MODE_MULTI,
                                MultiImageSelectorActivity.EXTRA_DEFAULT_SELECTED_LIST to selected
                        )
                    },
                    doOnRefuse = { toast("没有获得权限哦~") }
            )
        }

        images.setOnClickDeletecteListener { _, position ->
            imageList.removeAt(position)
            images.setImagesData(imageList)
        }
    }

    private fun upload() {
        //todo 待测试
        //todo notify pre activity item
        val user = BaseAPP.getUser(this)
        val files = mutableListOf<File>()
        imageList.map { File(it.url) }
                .toCollection(files)
        if (content.text.isBlank()) {
            toast("回答不能为空哦~")
        }
        RequestManager.INSTANCE.answerQuestion(SimpleObserver<Unit>(this, true, object : SubscriberListener<Unit>(QAErrorHandler) {
            override fun onNext(t: Unit?) {
                super.onNext(t)
                toast("发布成功")
                saveCache = false

                if (draftId != null) {
                    val data = Intent()
                    data.putExtra("id", draftId)
                    setResult(DraftActivity.RESULT_DRAFT_SUBMITTED, data)
                } else {
                    setResult(Activity.RESULT_OK)
                }
                finish()
            }
        }), user.stuNum, user.idNum, qid, content.text.toString(), files)
    }

    private fun saveToDraft() {
        val str = content.text.toString()
        if (str.isBlank() || cache == str) return
        cache = str
        toast("您未提交的内容将提交至草稿箱")
        val user = BaseAPP.getUser(this)
        if (draftId != null) {
            RequestManager.INSTANCE.refreshDraft(SimpleObserver(BaseAPP.getContext(), object : SubscriberListener<Unit>(QAErrorHandler) {
                override fun onError(e: Throwable?): Boolean {
                    BaseAPP.getContext().longToast("更新草稿失败，请在app退出前重新尝试或直接提交, app退出后记录将丢失")
                    return true
                }
            }), user.stuNum, user.idNum, str, draftId)
        } else {
            RequestManager.INSTANCE.addDraft(SimpleObserver(BaseAPP.getContext(), object : SubscriberListener<Unit>(QAErrorHandler) {
                override fun onError(e: Throwable?): Boolean {
                    BaseAPP.getContext().longToast("保存至草稿箱失败，请在app退出前重新尝试或直接提交, app退出后记录将丢失")
                    return true
                }
            }), user.stuNum, user.idNum, Draft.TYPE_ANSWER, str, qid)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == SELECT_IMAGE) {
            val result = data?.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT)
                    ?: mutableListOf<String>()
            imageList.forEach { result.remove(it.url) }
            result.forEach { imageList.add(Image(it, Image.TYPE_NORMAL)) }
            if (imageList.size > MAX_IMAGE_NUM) {
                imageList.removeAt(MAX_IMAGE_NUM)
                toast("最多只能选择${MAX_IMAGE_NUM}张图片哦~")
            }
            images.setImagesData(imageList)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (saveCache) {
            setResult(DraftActivity.RESULT_DRAFT_REFRESHED)
            saveToDraft()
        }
    }
}
