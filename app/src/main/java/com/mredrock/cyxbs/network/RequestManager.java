package com.mredrock.cyxbs.network;

import android.content.Context;

import com.mredrock.cyxbs.BaseAPP;
import com.mredrock.cyxbs.BuildConfig;
import com.mredrock.cyxbs.component.remind_service.Reminder;
import com.mredrock.cyxbs.component.remind_service.func.BaseRemindFunc;
import com.mredrock.cyxbs.config.Const;
import com.mredrock.cyxbs.event.AskLoginEvent;
import com.mredrock.cyxbs.model.AboutMe;
import com.mredrock.cyxbs.model.Affair;
import com.mredrock.cyxbs.model.BaseRedrockApiWrapper;
import com.mredrock.cyxbs.model.Course;
import com.mredrock.cyxbs.model.ElectricCharge;
import com.mredrock.cyxbs.model.Empty;
import com.mredrock.cyxbs.model.EmptyRoom;
import com.mredrock.cyxbs.model.Exam;
import com.mredrock.cyxbs.model.Food;
import com.mredrock.cyxbs.model.FoodComment;
import com.mredrock.cyxbs.model.FoodDetail;
import com.mredrock.cyxbs.model.Grade;
import com.mredrock.cyxbs.model.PastElectric;
import com.mredrock.cyxbs.model.RedrockApiWrapper;
import com.mredrock.cyxbs.model.RollerViewInfo;
import com.mredrock.cyxbs.model.SchoolCarLocation;
import com.mredrock.cyxbs.model.Shake;
import com.mredrock.cyxbs.model.StartPage;
import com.mredrock.cyxbs.model.UpdateInfo;
import com.mredrock.cyxbs.model.User;
import com.mredrock.cyxbs.model.VolunteerTime;
import com.mredrock.cyxbs.model.help.MyQuestion;
import com.mredrock.cyxbs.model.help.Question;
import com.mredrock.cyxbs.model.help.QuestionId;
import com.mredrock.cyxbs.model.lost.Lost;
import com.mredrock.cyxbs.model.lost.LostDetail;
import com.mredrock.cyxbs.model.lost.LostStatus;
import com.mredrock.cyxbs.model.lost.LostWrapper;
import com.mredrock.cyxbs.model.qa.Answer;
import com.mredrock.cyxbs.model.qa.CheckInStatus;
import com.mredrock.cyxbs.model.qa.Draft;
import com.mredrock.cyxbs.model.qa.QuestionDetail;
import com.mredrock.cyxbs.model.qa.RelateMeItem;
import com.mredrock.cyxbs.model.social.BBDDNewsContent;
import com.mredrock.cyxbs.model.social.CommentContent;
import com.mredrock.cyxbs.model.social.HotNews;
import com.mredrock.cyxbs.model.social.OfficeNewsContent;
import com.mredrock.cyxbs.model.social.PersonInfo;
import com.mredrock.cyxbs.model.social.PersonLatest;
import com.mredrock.cyxbs.model.social.Topic;
import com.mredrock.cyxbs.model.social.TopicArticle;
import com.mredrock.cyxbs.model.social.UploadImgResponse;
import com.mredrock.cyxbs.network.exception.RedrockApiException;
import com.mredrock.cyxbs.network.func.AffairTransformFunc;
import com.mredrock.cyxbs.network.func.AffairWeekFilterFunc;
import com.mredrock.cyxbs.network.func.ElectricQueryFunc;
import com.mredrock.cyxbs.network.func.RedrockApiNoDataWrapperFunc;
import com.mredrock.cyxbs.network.func.RedrockApiWrapperFunc;
import com.mredrock.cyxbs.network.func.StartPageFunc;
import com.mredrock.cyxbs.network.func.UpdateVerifyFunc;
import com.mredrock.cyxbs.network.func.UserCourseFilterFunc;
import com.mredrock.cyxbs.network.func.UserInfoVerifyFunc;
import com.mredrock.cyxbs.network.interceptor.StudentNumberInterceptor;
import com.mredrock.cyxbs.network.observable.CourseListProvider;
import com.mredrock.cyxbs.network.observable.EmptyRoomListProvider;
import com.mredrock.cyxbs.network.service.LostApiService;
import com.mredrock.cyxbs.network.service.QAService;
import com.mredrock.cyxbs.network.service.RedrockApiService;
import com.mredrock.cyxbs.network.service.VolunteerService;
import com.mredrock.cyxbs.network.setting.CacheProviders;
import com.mredrock.cyxbs.network.setting.QualifiedTypeConverterFactory;
import com.mredrock.cyxbs.ui.activity.lost.LostActivity;
import com.mredrock.cyxbs.ui.fragment.social.TopicFragment;
import com.mredrock.cyxbs.util.BitmapUtil;
import com.mredrock.cyxbs.util.SchoolCalendar;
import com.mredrock.cyxbs.util.Utils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.rx_cache2.DynamicKey;
import io.rx_cache2.EvictDynamicKey;
import io.rx_cache2.Reply;
import io.rx_cache2.internal.RxCache;
import io.victoralbertos.jolyglot.JacksonSpeaker;
import kotlin.Unit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;


public enum RequestManager {

    INSTANCE;

    private static final int DEFAULT_TIMEOUT = 30;
    private RedrockApiService redrockApiService;
    private LostApiService lostApiService;
    private CacheProviders cacheProviders;
    private OkHttpClient okHttpClient;
    private VolunteerService volunteerService;
    private QAService qaService;

    RequestManager() {
        okHttpClient = configureOkHttp(new OkHttpClient.Builder());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Const.END_POINT_REDROCK)
                .client(okHttpClient)
                .addConverterFactory(new QualifiedTypeConverterFactory(
                        GsonConverterFactory.create(), SimpleXmlConverterFactory.create()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        cacheProviders = new RxCache.Builder()
                .persistence(BaseAPP.getContext().getFilesDir(), new JacksonSpeaker())
                .using(CacheProviders.class);

        redrockApiService = retrofit.create(RedrockApiService.class);
        lostApiService = retrofit.create(LostApiService.class);
        volunteerService = retrofit.create(VolunteerService.class);
        qaService = retrofit.create(QAService.class);

//        Retrofit volunteerRetrofit = new Retrofit.Builder()
//                .baseUrl(Const.API_VOLUNTEER)
//                .client(okHttpClient)
//                .addConverterFactory(new QualifiedTypeConverterFactory(
//                        GsonConverterFactory.create(), SimpleXmlConverterFactory.create()))
//                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
//                .build();

    }

    public static RequestManager getInstance() {
        return INSTANCE;
    }

    public OkHttpClient configureOkHttp(OkHttpClient.Builder builder) {
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
        builder.addInterceptor(new StudentNumberInterceptor());

        return builder.build();
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public void checkUpdate(Observer<UpdateInfo> observer, int versionCode) {

        Observable<UpdateInfo> observable = redrockApiService.update()
                .map(new UpdateVerifyFunc(versionCode));

        emitObservable(observable, observer);
    }

    public void login(Observer<User> observer, String stuNum, String idNum) {
        Observable<User> observable = redrockApiService.verify(stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>())
                .zipWith(redrockApiService.getPersonInfo(stuNum, idNum)
                        .map(new RedrockApiWrapperFunc<>()), User::cloneFromUserInfo);

        emitObservable(observable, observer);
    }

    public void getNowWeek(Observer<Integer> observer, String stuNum, String idNum) {
        Observable<Integer> observable = redrockApiService.getCourse(stuNum, idNum, "0")
                .map(courseWrapper -> {
                    if (courseWrapper.status != Const.REDROCK_API_STATUS_SUCCESS) {
                        throw new RedrockApiException();
                    }
                    return Integer.parseInt(courseWrapper.nowWeek);
                });
        emitObservable(observable, observer);
    }

    public void getVolunteer(Observer<VolunteerTime> subscriber, String account, String password) {
        Observable<VolunteerTime> observable = volunteerService.getVolunteerUseLogin(account, password);
        emitObservable(observable, subscriber);
    }

    public void getVolunteerTime(Observer<VolunteerTime.DataBean> subscriber, String uid) {
        Observable<VolunteerTime.DataBean> observable = volunteerService.getVolunteerUseUid(uid)
                .map(VolunteerTime::getData);
        emitObservable(observable, subscriber);
    }

    public void getSchoolCarLocation(Observer<SchoolCarLocation> subscriber) {
        Observable<SchoolCarLocation> observable = redrockApiService.schoolcar();
        emitObservable(observable, subscriber);
    }

    public void getCourseList(Observer<List<Course>> observer, String stuNum, String idNum, int week, boolean update) {
//        Observable<List<Course>> observable = CourseListProvider.start(stuNum, idNum, update,false)
//                .map(new UserCourseFilterFunc(week));

        getCourseList(observer, stuNum, idNum, week, update, true);
    }

    public void getCourseList(Observer<List<Course>> observer, String stuNum, String idNum,
                              int week, boolean update, boolean forceFetch) {
        Observable<List<Course>> observable = CourseListProvider.start(stuNum, idNum, update, forceFetch)
                .map(new UserCourseFilterFunc(week));
        emitObservable(observable, observer);
    }

    public void getRemindableList(Observer<List<Reminder>> observer, Context context, BaseRemindFunc remindFunc) {
        Observable<List<Reminder>> observable = CourseListProvider.start(BaseAPP.getUser(context).stuNum, BaseAPP.getUser(context).idNum, false, false)
                .map(new UserCourseFilterFunc(new SchoolCalendar()
                        .getWeekOfTerm()))
                .map(remindFunc);
        emitObservable(observable, observer);
    }

    public Observable<List<Course>> getCourseList(String stuNum, String idNum) {
        return redrockApiService.getCourse(stuNum, idNum, "0").map(new RedrockApiWrapperFunc<>());
    }

    public List<Course> getCourseListSync(String stuNum, String idNum, boolean forceFetch) throws IOException {
        Response<Course.CourseWrapper> response = redrockApiService.getCourseCall(stuNum, idNum, "0", forceFetch).execute();
        return response.body().data;
    }

//    public CompositeDisposable getMapOverlayImageUrl(Observer<String> observer, String name, String path) {
//        Observable<String> observable = redrockApiService.getMapOverlayImageUrl(name, path)
//                .map(wrapper -> {
//                    if (wrapper.status != 204) {
//                        throw new RedrockApiException(wrapper.info);
//                    } else {
//                        return wrapper.data;
//                    }
//                })
//                .flatMap(urlList -> Observable.just(urlList.get(0)));
//        return emitObservable(observable, observer);
//    }

    public Disposable getShake(DisposableObserver<Shake> observer) {
        Observable<Shake> observable = redrockApiService.getShake()
                .map(new RedrockApiWrapperFunc<>());

        return emitObservable(observable, observer);
    }

    public Disposable getFoodList(DisposableObserver<List<Food>> observer, String page, String defaultIntro) {
        Observable<List<Food>> observable = redrockApiService.getFoodList(page)
                .map(new RedrockApiWrapperFunc<>())
                .filter(Utils::checkNotNullAndNotEmpty)
                .flatMap(foodList -> {
                    for (Food food : foodList) {
                        redrockApiService.getFoodDetail(food.id)
                                .map(new RedrockApiWrapperFunc<>())
                                .filter(foodDetail -> foodDetail != null)
                                .onErrorReturn(throwable -> {
                                    FoodDetail defaultFoodDetail = new FoodDetail();
                                    defaultFoodDetail.shop_content = defaultIntro;
                                    return defaultFoodDetail;
                                })
                                .doOnNext(foodDetail -> foodDetail.shop_content =
                                        foodDetail.shop_content
                                                .replaceAll("\t", "")
                                                .replaceAll("\r\n", ""))
                                .subscribe(foodDetail -> {
                                    food.introduction = foodDetail.shop_content;
                                });
                    }

                    return Observable.just(foodList);
                });

        return emitObservable(observable, observer);
    }


    public Disposable getFoodAndCommentList(DisposableObserver<FoodDetail> observer, String shopId, String page) {

        Observable<FoodDetail> observable = redrockApiService.getFoodDetail(shopId)
                .map(new RedrockApiWrapperFunc<>())
                .filter(foodDetail -> foodDetail != null)
                .doOnNext(foodDetail -> {
                    foodDetail.shop_content = foodDetail.shop_content.replaceAll("\t", "").replaceAll("\r\n", "");
                    foodDetail.shop_tel = foodDetail.shop_tel.trim();
                })
                .flatMap(foodDetail -> {
                    redrockApiService.getFoodComments(shopId, page)
                            .map(new RedrockApiWrapperFunc<>())
                            .filter(Utils::checkNotNullAndNotEmpty)
                            .onErrorReturn(throwable -> new ArrayList<>())
                            .flatMap(Observable::fromIterable)
                            .toSortedList()
                            .subscribe(foodCommentList -> {
                                foodDetail.foodComments = foodCommentList;
                            });
                    return Observable.just(foodDetail);
                });

        return emitObservable(observable, observer);
    }

    public Disposable getFood(DisposableObserver<FoodDetail> observer, String restaurantKey) {
        Observable<FoodDetail> observable = redrockApiService.getFoodDetail(restaurantKey)
                .map(new RedrockApiWrapperFunc<>());
        return emitObservable(observable, observer);
    }

    public Disposable sendCommentAndRefresh(DisposableObserver<List<FoodComment>> observer, String shopId, String userId, String userPassword, String content, String authorName) {
        Observable<RedrockApiWrapper<Object>> sendObservable = redrockApiService.sendFoodComment(shopId, userId, userPassword, content, authorName);
        Observable<List<FoodComment>> foodCommentObservable =
                redrockApiService.getFoodComments(shopId, "1").map(new RedrockApiWrapperFunc<>())
                        .filter(Utils::checkNotNullAndNotEmpty)
                        .flatMap(Observable::fromIterable)
                        .toSortedList()
                        .toObservable();
        Observable<List<FoodComment>> observable = Observable.zip(sendObservable, foodCommentObservable,
                (wrapper, foodCommentList) -> {
                    if (wrapper.status == Const.REDROCK_API_STATUS_SUCCESS) {
                        return foodCommentList;
                    } else {
                        return null;
                    }
                });

        return emitObservable(observable, observer);
    }

    public Disposable getFoodCommentList(DisposableObserver<List<FoodComment>> observer
            , String shopId, String page) {
        Observable<List<FoodComment>> observable =
                redrockApiService.getFoodComments(shopId, page)
                        .map(new RedrockApiWrapperFunc<>())
                        .filter(Utils::checkNotNullAndNotEmpty)
                        .flatMap(Observable::fromIterable)
                        .toSortedList()
                        .toObservable();

        return emitObservable(observable, observer);
    }

    public void getPublicCourse(Observer<List<Course>> observer,
                                List<String> stuNumList, String week) {
        Observable<List<Course>> observable = Observable.fromIterable(stuNumList)
                .flatMap(s -> redrockApiService.getCourse(s, "", week))
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }


    public void getStudent(Observer<List<com.mredrock.cyxbs.model.Student>> observer,
                           String stu) {
        Observable<List<com.mredrock.cyxbs.model.Student>> observable = redrockApiService.getStudent(stu)
                .map(studentWrapper -> studentWrapper.data);
        emitObservable(observable, observer);
    }

//    public void getEmptyRoomList(Subscriber<List<String>> subscriber, String
//            buildNum, String week, String weekdayNum, String sectionNum) {
//        Observable<List<String>> observable = redrockApiService
//                .getEmptyRoomList(buildNum, week, weekdayNum, sectionNum)
//                .map(new RedrockApiWrapperFunc<>());
//        emitObservable(observable, subscriber);
//    }

    public void queryEmptyRoomList(Observer<List<EmptyRoom>> observer, int week, int weekday, int build, int[] sections) {
        Observable<List<EmptyRoom>> observable = EmptyRoomListProvider.INSTANCE
                .createObservable(week, weekday, build, sections);
        emitObservable(observable, observer);
    }

    public List<String> getEmptyRoomListSync(int week, int weekday, int build, int section) throws IOException {
        Response<Empty> response = redrockApiService.getEmptyRoomListCall(week, weekday, build, section).execute();
        return response.body().data;
    }

    public void getGradeList(Observer<List<Grade>> observer, String
            stuNum, String stuId, boolean update) {
        Observable<List<Grade>> observable = redrockApiService.getGrade(stuNum, stuId)
                .map(new RedrockApiWrapperFunc<>());
        cacheProviders.getCachedGradeList(observable, new DynamicKey
                (stuNum), new EvictDynamicKey(update))
                .map(Reply::getData);
        emitObservable(observable, observer);
    }

    public void getExamList(Observer<List<Exam>> observer, String
            stu, boolean update) {
        Observable<List<Exam>> observable = redrockApiService.getExam(stu).map(
                examWapper -> examWapper.data);
        cacheProviders.getCachedExamList(observable, new DynamicKey(stu), new
                EvictDynamicKey(update))
                .map(Reply::getData);
        emitObservable(observable, observer);
    }

    public void getReExamList(Observer<List<Exam>> observer, String
            stu, boolean update) {
        Observable<List<Exam>> observable = redrockApiService.getReExam(stu).map(
                examWapper -> examWapper.data);
        cacheProviders.getCachedExamList(observable, new DynamicKey(stu), new
                EvictDynamicKey(update))
                .map(Reply::getData);
        emitObservable(observable, observer);
    }

    public void getAboutMeList(Observer<List<AboutMe>> observer, String stuNum, String idNum) {
        Observable<List<AboutMe>> observable = redrockApiService.getAboutMe(stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getTrendDetail(Observer<List<HotNews>> observer, int type_id, String article_id) {
        List<HotNews> newsList = new ArrayList<>();
        Observable<List<HotNews>> observable = redrockApiService.getTrendDetail(type_id, article_id)
                .flatMap(bbddDetailWrapper -> Observable.fromIterable(bbddDetailWrapper.data))
                .map(bbddDetail -> {
                    HotNews news = new HotNews(bbddDetail);
                    newsList.add(news);
                    return newsList;
                });
        emitObservable(observable, observer);
    }

    public void getMyTrend(Observer<List<HotNews>> observer, String stuNum, String idNum) {
        List<HotNews> newsList = new ArrayList<>();
        Observable<List<HotNews>> observable = redrockApiService.searchTrends(stuNum, idNum)
                .flatMap(mDetailWrapper -> Observable.fromIterable(mDetailWrapper.data))
                .map(mDetail -> {
                    HotNews news = new HotNews(mDetail);
                    newsList.add(news);
                    return newsList;
                })
                .map(hotNewses -> {
                    for (HotNews h : hotNewses) {
                        h.data.nickName = BaseAPP.getUser(BaseAPP.getContext()).getNickname();
                        h.data.userHead = BaseAPP.getUser(BaseAPP.getContext()).photo_thumbnail_src;
                    }
                    return hotNewses;
                });
        emitObservable(observable, observer);
    }

    /**
     * api
     */
    public Observable<UploadImgResponse.Response> uploadNewsImg(String stuNum, String filePath) {
        File file = new File(filePath);
        try {
            file = BitmapUtil.decodeBitmapFromRes(BaseAPP.getContext(), filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part file_body = MultipartBody.Part.createFormData("fold", file.getName(), requestFile);
        RequestBody stuNum_body = RequestBody.create(MediaType.parse("multipart/form-data"), stuNum);
        return redrockApiService.uploadSocialImg(stuNum_body, file_body)
                .map(new RedrockApiWrapperFunc<>());
    }

    public void getHotArticle(Observer<List<HotNews>> observer, int size, int page) {
        Observable<List<HotNews>> observable = redrockApiService.getSocialHotList(size, page);
        emitObservable(observable, observer);
    }

    public void getListNews(Observer<List<HotNews>> observer, int size, int page) {
        Observable<List<HotNews>> observable = redrockApiService.getSocialOfficialNewsList(size, page)
                .map(new RedrockApiWrapperFunc<>())
                .map(officeNewsContentList -> {
                    List<HotNews> aNews = new ArrayList<>();
                    for (OfficeNewsContent officeNewsContent : officeNewsContentList)
                        aNews.add(new HotNews(officeNewsContent));
                    return aNews;
                });
        emitObservable(observable, observer);
    }

    public void getListArticle(Observer<List<HotNews>> observer, int type_id, int size, int page) {
        Observable<List<HotNews>> observable = redrockApiService.getSocialBBDDList(type_id, size, page)
                .map(new RedrockApiWrapperFunc<>())
                .flatMap(bbdd -> Observable.just(bbdd)
                        .map(mBBDD -> {
                            List<HotNews> aNews = new ArrayList<>();
                            for (BBDDNewsContent bbddNewsContent : mBBDD)
                                aNews.add(new HotNews(bbddNewsContent));
                            return aNews;
                        }));
        emitObservable(observable, observer);
    }

    public Observable<String> sendDynamic(int type_id,
                                          String title,
                                          String content,
                                          String thumbnail_src,
                                          String photo_src,
                                          String user_id,
                                          String stuNum,
                                          String idNum) {

        return redrockApiService.sendDynamic(type_id, title, user_id, content, thumbnail_src, photo_src, stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> sendTopicArticle(int topicId,
                                               String title,
                                               String content,
                                               String thumbnailSrc,
                                               String photoSrc,
                                               String stuNum,
                                               String idNum
    ) {
        return redrockApiService.sendTopicArticle(topicId, title, content, thumbnailSrc, photoSrc, stuNum, idNum, false)
                .map(new RedrockApiWrapperFunc<>()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public void getRemarks(Observer<List<CommentContent>> observer,
                           String article_id,
                           int type_id) {
        Observable<List<CommentContent>> observable = redrockApiService.getSocialCommentList(article_id, type_id)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }


    public void postReMarks(Observer<String> observer,
                            String article_id,
                            int type_id,
                            String content,
                            String user_id,
                            String stuNum,
                            String idNum) {
        if (!checkWithUserId("没有完善信息,还想发回复？")) return;
        Observable<String> observable = redrockApiService.addSocialComment(article_id, type_id, content, user_id, stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void addThumbsUp(Observer<String> observer,
                            String article_id,
                            int type_id,
                            String stuNum,
                            String idNum) {
        if (!checkWithUserId("没有完善信息,肯定不让你点赞呀")) return;
        Observable<String> observable = redrockApiService.socialLike(article_id, type_id, stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void cancelThumbsUp(Observer<String> observer,
                               String article_id,
                               int type_id,
                               String stuNum,
                               String idNum) {
        if (!checkWithUserId("没有完善信息,肯定不让你点赞呀")) return;
        Observable<String> observable = redrockApiService.socialUnlike(article_id, type_id, stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    @SuppressWarnings("unchecked")
    public Observable<RedrockApiWrapper> setPersonInfo(String stuNum, String idNum, String photo_thumbnail_src, String photo_src) {

        return redrockApiService.setPersonInfo(stuNum, idNum, photo_thumbnail_src, photo_src);
    }

    @SuppressWarnings("unchecked")
    public void setPersonNickName(Observer<RedrockApiWrapper<Object>> observer, String stuNum, String idNum, String nickName) {
        Observable<RedrockApiWrapper<Object>> observable = redrockApiService.setPersonNickName(stuNum, idNum, nickName);
        emitObservable(observable, observer);
    }


    public void getPersonInfo(Observer<PersonInfo> observer, String otherStuNum, String stuNum, String idNum) {
        Observable<PersonInfo> observable = redrockApiService.getPersonInfo(otherStuNum, stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getPersonLatestList(Observer<List<HotNews>> observer, String otherStuNum, String userName, String userHead) {
        Observable<List<HotNews>> observable = redrockApiService.getPersonLatestList(otherStuNum)
                .map(new RedrockApiWrapperFunc<>())
                .map(latestsList -> {
                    List<HotNews> aNews = new ArrayList<>();
                    for (PersonLatest personLatest : latestsList)
                        aNews.add(new HotNews(personLatest, otherStuNum, userName, userHead));
                    return aNews;
                });
        emitObservable(observable, observer);
    }

    @SuppressWarnings("unchecked")
    public void setPersonIntroduction(Observer<RedrockApiWrapper<Object>> observer, String stuNum, String idNum, String introduction) {

        Observable<RedrockApiWrapper<Object>> observable = redrockApiService.setPersonIntroduction(stuNum, idNum, introduction);

        emitObservable(observable, observer);
    }

    @SuppressWarnings("unchecked")
    public void setPersonQQ(Observer<RedrockApiWrapper<Object>> observer, String stuNum, String idNum, String qq) {

        Observable<RedrockApiWrapper<Object>> observable = redrockApiService.setPersonQQ(stuNum, idNum, qq)
                .map(new RedrockApiWrapperFunc());

        emitObservable(observable, observer);
    }

    @SuppressWarnings("unchecked")
    public void setPersonPhone(Observer<RedrockApiWrapper<Object>> observer, String stuNum, String idNum, String phone) {

        Observable<RedrockApiWrapper<Object>> observable = redrockApiService.setPersonPhone(stuNum, idNum, phone)
                .map(new RedrockApiWrapperFunc());

        emitObservable(observable, observer);
    }

    public void getPersonInfo(Observer<User> observer, String stuNum, String idNum) {
        Observable<User> observable = redrockApiService.getPersonInfo(stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>())
                .map(new UserInfoVerifyFunc());
        emitObservable(observable, observer);
    }

    public void getAffair(Observer<List<Affair>> observer, String stuNum, String idNum) {
        Observable<List<Affair>> observable = redrockApiService.getAffair(stuNum, idNum)
                .map(new AffairTransformFunc());
        emitObservable(observable, observer);
    }

    public void getAffair(Observer<List<Affair>> observer, String stuNum, String idNum, int week) {
        Observable<List<Affair>> observable = redrockApiService.getAffair(stuNum, idNum)
                .map(new AffairTransformFunc())
                .map(new AffairWeekFilterFunc(week));
        emitObservable(observable, observer);
    }

    public void addAffair(Observer<Object> observer, String stuNum, String idNum, String uid, String title,
                          String content, String date, int time) {
        Observable<Object> observable = redrockApiService.addAffair(uid, stuNum, idNum, date, time, title, content)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }


    public void editAffair(Observer<Object> observer, String stuNum, String idNum, String uid, String title,
                           String content, String date, int time) {
        Observable<Object> observable = redrockApiService.editAffair(uid, stuNum, idNum, date, time, title, content)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void deleteAffair(Observer<Object> observer, String stuNum, String idNum, String uid) {
        Observable<Object> observable = redrockApiService.deleteAffair(stuNum, idNum, uid)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getStartPage(Observer<StartPage> observer) {
        Observable<StartPage> observable = redrockApiService.startPage()
                .map(new RedrockApiWrapperFunc<>())
                .map(new StartPageFunc());
        emitObservable(observable, observer);
    }

    public void queryElectricCharge(Observer<ElectricCharge> observer, String building, String room) {
        if (!checkWithUserId("需要先登录才能发送失物招领信息哦")) return;
        Observable<ElectricCharge> observable = redrockApiService.queryElectricCharge(building, room)
                .map(new ElectricQueryFunc());
        emitObservable(observable, observer);
    }


    public void bindDormitory(String stuNum, String idNum, String room, Observer<Object> subscriber) {
        if (!checkWithUserId("需要先登录才能查询绑定寝室哦"))
            return;
        Observable<Object> observable = redrockApiService.bindDormitory(stuNum, idNum, room)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, subscriber);
    }

    public void queryPastElectricCharge(String stuNum, String idNum, Observer<PastElectric.PastElectricResultWrapper> subscriber) {
        if (!checkWithUserId("需要先登录才能查询绑定寝室哦"))
            return;
        Observable<PastElectric.PastElectricResultWrapper> observable = redrockApiService.getPastElectricCharge(stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, subscriber);
    }

    public void getLostList(Observer<LostWrapper<List<Lost>>> observer, int theme, String category, int page) {
        String themeString;
        if (theme == LostActivity.THEME_LOST) {
            themeString = "lost";
        } else {
            themeString = "found";
        }
        Observable<LostWrapper<List<Lost>>> observable = lostApiService.getLostList(themeString, category, page);
        emitObservable(observable, observer);
    }

    public void getLostDetail(Observer<LostDetail> observer, Lost origin) {
        Observable<LostDetail> observable = lostApiService.getLostDetial(origin.id)
                .map(lostDetail -> lostDetail.mergeLost(origin));
        emitObservable(observable, observer);
    }

    public void createLost(Observer<LostStatus> observer, LostDetail detail, int theme) {
        if (!checkWithUserId("需要先登录才能发送失物招领信息哦")) return;
        User user = BaseAPP.getUser(BaseAPP.getContext());
        String themeString;
        if (theme == LostActivity.THEME_LOST) {
            themeString = "寻物启事";
        } else {
            themeString = "失物招领";
        }
        Observable<LostStatus> observable = lostApiService.create(user.stuNum,
                user.idNum,
                themeString,
                detail.category,
                detail.description,
                detail.time,
                detail.place,
                detail.connectPhone,
                detail.connectWx);
        emitObservable(observable, observer);
    }

    public void getTopicList(Observer<List<Topic>> observer, int size, int page, String stuNum, String idNum, String type) {
        Observable<List<Topic>> observable;
        switch (type) {
            case TopicFragment.TopicType.MY_TOPIC:
                observable = redrockApiService.getMyTopicList(stuNum, idNum, size, page)
                        .map(new RedrockApiWrapperFunc<>());
                break;
            case TopicFragment.TopicType.ALL_TOPIC:
                observable = redrockApiService.getAllTopicList(stuNum, idNum, size, page)
                        .map(new RedrockApiWrapperFunc<>());
                break;
            default:
                observable = redrockApiService.searchTopic(stuNum, idNum, size, page, type)
                        .map(new RedrockApiWrapperFunc<>());
                break;
        }
        emitObservable(observable, observer);
    }

    public void getTopicArticle(Observer<TopicArticle> observer, int size, int page, String stuNum, String idNum, int topicId) {
        Observable<TopicArticle> observable = redrockApiService.getTopicArticle(stuNum, idNum, size, page, topicId)
                .map(new RedrockApiWrapperFunc<>());

        emitObservable(observable, observer);
    }

    //Q&A
    public void getAnswerList(Observer<QuestionDetail> observer, String stuNum, String idNum, String qid) {
        Observable<QuestionDetail> observable = qaService.getAnswerList(stuNum, idNum, qid)
                .map(new RedrockApiWrapperFunc<QuestionDetail>() {
                    @Override
                    public QuestionDetail apply(RedrockApiWrapper<QuestionDetail> wrapper) throws Exception {
                        QuestionDetail data = super.apply(wrapper);
                        data.setHasAdoptedAnswer(false);
                        if (data.hasAnswer()) {
                            List<Answer> answers = data.getAnswers();
                            assert answers != null; //method hasAnswer() has ensure it's not null
                            for (Answer item : answers) {
                                if (item.isAdopted()) {
                                    data.setHasAdoptedAnswer(true);
                                    break;
                                }
                            }
                        }
                        return data;
                    }
                });
        emitObservable(observable, observer);
    }

    public void loadMoreAnswer(Observer<List<Answer>> observer, String stuNum, String idNum, String qid, int page) {
        //获取问题详情的接口返回的回答数量和这里size必须保持一致！
        Observable<List<Answer>> observable = qaService.loadMoreAnswer(stuNum, idNum, qid, page, "6")
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void praiseAnswer(Observer<Unit> observer, String stuNum, String idNum, String aid, boolean isPraised) {
        Observable<Unit> observable;
        if (isPraised) {
            observable = qaService.cancelPraiseAnswer(stuNum, idNum, aid)
                    .map(new RedrockApiNoDataWrapperFunc());
        } else {
            observable = qaService.praiseAnswer(stuNum, idNum, aid)
                    .map(new RedrockApiNoDataWrapperFunc());
        }
        emitObservable(observable, observer);
    }

    public void acceptAnswer(Observer<Unit> observer, String stuNum, String idNum, String aid, String qid) {
        Observable<Unit> observable = qaService.acceptAnswer(stuNum, idNum, aid, qid)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void answerQuestion(Observer<Unit> observer, String stuNum, String idNum, String qid, String content, List<File> files) {

        Observable<Unit> observable;
        if (files == null || files.isEmpty()) {
            observable = qaService.answerQuestion(stuNum, idNum, qid, content)
                    .map(new RedrockApiWrapperFunc<>())
                    .map(it -> Unit.INSTANCE);
        } else {
            observable = qaService.answerQuestion(stuNum, idNum, qid, content)
                    .map(new RedrockApiWrapperFunc<>())
                    .flatMap(it -> uploadPic(stuNum, idNum, it, files))
                    .map(new RedrockApiNoDataWrapperFunc());
        }
        emitObservable(observable, observer);
    }

    private Observable<BaseRedrockApiWrapper> uploadPic(String stuNum, String idNum, String aid, List<File> files) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("stuNum", stuNum)
                .addFormDataPart("idNum", idNum)
                .addFormDataPart("answer_id", aid);
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String suffix = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            RequestBody imageBody = RequestBody.create(MediaType.parse("image/" + suffix), file);
            String name = "photo" + (i + 1);
            builder.addFormDataPart(name, name, imageBody);
        }
        return qaService.uploadAnswerPic(builder.build().parts());
    }

    public void cancelQuestion(Observer<Unit> observer, String stuNum, String idNum, String qid) {
        Observable<Unit> observable = qaService.cancelQuestion(stuNum, idNum, qid)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void getAnswerCommentList(Observer<List<Answer>> observer, String stuNum, String idNum, String aid) {
        Observable<List<Answer>> observable = qaService.getAnswerCommentList(stuNum, idNum, aid)
                .map(wrapper -> {
                    List<Answer> list = new RedrockApiWrapperFunc<List<Answer>>().apply(wrapper);
                    if (list == null) {
                        return new ArrayList<>();
                    }
                    return list;
                });
        emitObservable(observable, observer);
    }

    public void commentAnswer(Observer<Unit> observer, String stuNum, String idNum, String aid, String content) {
        Observable<Unit> observable = qaService.commentAnswer(stuNum, idNum, aid, content)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void getDraftList(Observer<List<Draft>> observer, String stuNum, String idNum, int page) {
        Observable<List<Draft>> observable = qaService.getDraftList(stuNum, idNum, page, 6)
                .map(new RedrockApiWrapperFunc<List<Draft>>() {
                    @Override
                    public List<Draft> apply(RedrockApiWrapper<List<Draft>> wrapper) throws Exception {
                        List<Draft> drafts = super.apply(wrapper);
                        if (drafts == null) return Collections.emptyList();
                        for (Draft draft : drafts) {
                            if (draft.isQuestion()) {
                                draft.parseQuestion();
                            }
                        }
                        return drafts;
                    }
                });
        emitObservable(observable, observer);
    }

    public void addDraft(Observer<Unit> observer, String stuNum, String idNum, String type, String content, String targetId) {
        Observable<Unit> observable = qaService.addDraft(stuNum, idNum, type, content, targetId)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void refreshDraft(Observer<Unit> observer, String stuNum, String idNum, String content, String draftId) {
        Observable<Unit> observable = qaService.refreshDraft(stuNum, idNum, content, draftId)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void deleteDraft(Observer<Unit> observer, String stuNum, String idNum, String id) {
        Observable<Unit> observable = qaService.deleteDraft(stuNum, idNum, id)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void checkIn(Observer<Unit> observer, String stuNum, String idNum) {
        Observable<Unit> observable = qaService.checkIn(stuNum, idNum)
                .map(new RedrockApiNoDataWrapperFunc());
        emitObservable(observable, observer);
    }

    public void getCheckInStatus(Observer<CheckInStatus> observer, String stuNum, String idNum) {
        Observable<CheckInStatus> observable = qaService.getCheckInStatus(stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getRelateMeList(Observer<List<RelateMeItem>> observer, String stuNum, String idNum, int page, int type) {
        Observable<List<RelateMeItem>> observable = qaService.getRelateMeList(stuNum, idNum, page, 6, type)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    private <T> Disposable emitObservable(Observable<T> o, DisposableObserver<T> s) {
        return o.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(s);
    }

    private <T> void emitObservable(Observable<T> o, Observer<T> s) {
        o.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s);
    }


    public boolean checkWithUserId(String s) {
        if (!BaseAPP.isLogin()) {
            EventBus.getDefault().post(new AskLoginEvent(s));
            return false;
        } else {
            return true;
        }

    }

    public void getRollerViewInfo(Observer<List<RollerViewInfo>> observer, String pic_num) {
        Observable<List<RollerViewInfo>> observable = redrockApiService.getRollerViewInfo(pic_num)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getAllQuestion(Observer<List<Question>> observer, String page, String size, String kind) {
        Observable<List<Question>> observable = redrockApiService.getAllQuestion(page, size, kind)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }


    public void postNewQuestion(Observer observer, String stuNum, String idNum, Question q, List<String> files) {
        Observable<QuestionId> observable = redrockApiService.postNewQuestion(
                stuNum,
                idNum,
                q.getTitle(),
                q.getDescription(),
                q.getIs_anonymous(),
                q.getKind(),
                q.getTags(),
                q.getReward(),
                q.getDisappear_at())
                .map(new RedrockApiWrapperFunc<>());

        if (files.isEmpty()) {
            emitObservable(observable, observer);
        } else {
            observable.map(questionId -> questionId.id + "")
                    .subscribeOn(Schedulers.io())
                    .flatMap(questionId -> uploadHelpImg(stuNum, idNum, questionId, files))
                    .unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);

        }
    }

    public Observable<List<String>> uploadHelpImg(String stuNum, String idNum, String question_id, List<String> files) {
        List<MultipartBody.Part> parts = new ArrayList<>(files.size());
        int i = 1;
        for (String filePath : files) {
            File file = new File(filePath);
            try {
                file = BitmapUtil.decodeBitmapFromRes(BaseAPP.getContext(), filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            MultipartBody.Part file_body = MultipartBody.Part.createFormData("photo" + i, file.getName(), requestFile);
            parts.add(file_body);
            i++;
        }

        RequestBody stuNum_body = RequestBody.create(MediaType.parse("multipart/form-data"), stuNum);
        RequestBody idNum_body = RequestBody.create(MediaType.parse("multipart/form-data"), idNum);
        RequestBody questionId_body = RequestBody.create(MediaType.parse("multipart/form-data"), question_id);

        Observable<List<String>> observable = redrockApiService.uploadHelpImg(stuNum_body, idNum_body, questionId_body, parts)
                .map(new RedrockApiWrapperFunc<>());
        return observable;
    }

    public void getUserDiscountBalance(Observer<Integer> observer, String stuNum, String idNum) {
        Observable<Integer> observable = redrockApiService.getUserDiscountBalance(stuNum, idNum)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getMyHelp(Observer<List<MyQuestion>> observer, String stuNum, String idNum, int page, int size, int type) {
        Observable<List<MyQuestion>> observable = redrockApiService.getMyHelp(stuNum, idNum, page, size, type)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }

    public void getMyAsk(Observer<List<MyQuestion>> observer, String stuNum, String idNum, int page, int size, int type) {
        Observable<List<MyQuestion>> observable = redrockApiService.getMyAsk(stuNum, idNum, page, size, type)
                .map(new RedrockApiWrapperFunc<>());
        emitObservable(observable, observer);
    }
}

