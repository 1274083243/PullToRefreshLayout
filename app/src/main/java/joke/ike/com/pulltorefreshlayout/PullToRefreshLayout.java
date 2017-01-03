package joke.ike.com.pulltorefreshlayout;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import org.w3c.dom.ls.LSInput;

/**
 * Created by ike on 2016/12/29.
 * 下拉刷新控件
 */
public class PullToRefreshLayout extends LinearLayout {
    private String Tag = "PullToRefreshLayout";
    private View headView;
    private TextView tv_headView;
    private Scroller mScroll;
    private int headViewHeight;
    private Context context;
    private LinearLayout contentView;
    private RefreshListener listener;
    private boolean isRefreshing;
    private boolean isDraging;

    public PullToRefreshLayout(Context context) {
        this(context, null);
    }
    public PullToRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    private Handler handler=new Handler();
    public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroll=new Scroller(context);
        this.context=context;
    }
    private void initView() {
        contentView= (LinearLayout) findViewById(R.id.content);
        headView=findViewById(R.id.headView);
        tv_headView = (TextView)findViewById(R.id.tv_headView);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i=0;i<getChildCount();i++){
            View view=getChildAt(i);
            if (view.getId()==R.id.headView){
                view.layout(0,-headView.getMeasuredHeight(),getWidth(),0);
            }
            if (view.getId()==R.id.content){
                view.layout(0,0,getWidth(),contentView.getMeasuredHeight());
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        heightMeasureSpec=MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec)+headView.getMeasuredHeight(),MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int y= (int) ev.getY();
        switch (ev.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                lastY=y;
                break;
            case MotionEvent.ACTION_MOVE:
                int dy=y-lastY;
                if ((y-lastY)>0){//下滑
                    if (getScrollChild() instanceof AbsListView){
                        // 判断AbsListView是否已经到达内容最顶部(如果已经到达最顶部，就拦截事件，自己处理滑动)
                        if (isListViewToTop()){
                            ((AbsListView) getScrollChild()).requestDisallowInterceptTouchEvent(false);
                            Move(dy);
                        }
                    }
                }else {//上滑
                    if (getScrollY()==0){
                        ((AbsListView) getScrollChild()).requestDisallowInterceptTouchEvent(true);
                    }else {
                        Move(dy);
                        return true;
                    }
                }
                //向下拉动
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getScrollY() != 0) {
                    int distance = getScrollY() + headView.getMeasuredHeight();
                    if (Math.abs(getScrollY()) >= headView.getMeasuredHeight() / 2) {
                        mScroll.startScroll(0, getScrollY(), 0, -distance, 300);
                        handler.post(new AnnimationTask());
                        tv_headView.setText("刷新中");
                        if (listener != null) {
                            if (!isRefreshing) {
                                isRefreshing = true;
                                listener.onReresh();
                            }
                        }
                    } else {
                        mScroll.startScroll(0, getScrollY(), 0, distance, 300);
                        handler.post(new AnnimationTask());
                    }
                }
                break;
        }
        lastY=y;
        return super.dispatchTouchEvent(ev);
    }

    private boolean isListViewToTop() {
        AdapterView content= (AdapterView) getScrollChild();
        boolean b = ViewCompat.canScrollVertically(content, -1);
        return !b;
    }

    private View getScrollChild() {
        return contentView.getChildAt(0);
    }

    private void Move(int dy) {
        if (dy>0){
            if (Math.abs(getScrollY())>headView.getMeasuredHeight()){
                scrollBy(0,-dy/3);
                if (!isRefreshing){
                    tv_headView.setText("松开刷新");
                }

            }else if (Math.abs(getScrollY())>headView.getMeasuredHeight()/2){
                if (!isRefreshing){
                    tv_headView.setText("下拉刷新");
                }
                scrollBy(0, -dy);
            }else {
                scrollBy(0, -dy);
            }
        }else {
            scrollBy(0, -dy);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y>getScrollY()){//向上滑动
            if (y>=getScrollY()){
                y=getScrollY();
            }
        }
        super.scrollTo(x, y);
    }
    private  int mLastY;
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        boolean isIntercept=false;
//        int current_Y= (int) ev.getY();
//        switch (ev.getAction()){
//            case MotionEvent.ACTION_DOWN:
//                isIntercept=false;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                if ((current_Y-mLastY)>0){
//                    if (contentView.getChildAt(0) instanceof AbsListView){
//                        // 判断AbsListView是否已经到达内容最顶部(如果已经到达最顶部，就拦截事件，自己处理滑动)
//                        AdapterView content= (AdapterView) contentView.getChildAt(0);
//                        if (content.getFirstVisiblePosition() == 0
//                                || content.getChildAt(0).getTop() == 0) {
//                            isIntercept = true;
//                        }
//                    }
//                }else {
//                    if (isRefreshing){
//                        isIntercept=true;
//                    }
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                isIntercept=false;
//                break;
//        }
//        mLastY=current_Y;
//        return isIntercept;
//    }
    private int currntY;
    private int lastY;
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        currntY= (int) event.getY();
//        switch (event.getAction()){
//            case MotionEvent.ACTION_DOWN:
//               // mLastY=currntY;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                int dy= currntY-mLastY;
//                //向下拉动
//                if (dy>0){
//                    if (Math.abs(getScrollY())>headView.getMeasuredHeight()){
//                        scrollBy(0,-dy/3);
//                        if (!isRefreshing){
//                            tv_headView.setText("松开刷新");
//                        }
//
//                    }else if (Math.abs(getScrollY())>headView.getMeasuredHeight()/2){
//                        if (!isRefreshing){
//                            tv_headView.setText("下拉刷新");
//                        }
//                        scrollBy(0, -dy);
//                    }else {
//                        scrollBy(0, -dy);
//                    }
//                }else {
//                    scrollBy(0, -dy);
//                }
//
//                break;
//            case MotionEvent.ACTION_CANCEL:
//            case MotionEvent.ACTION_UP:
//                if(!isRefreshing){
//                int distance=getScrollY()+headView.getMeasuredHeight();
//                if (Math.abs(getScrollY())>=headView.getMeasuredHeight()/2){
//                    mScroll.startScroll(0,getScrollY(),0,-distance,300);
//                    handler.post(new AnnimationTask());
//                    tv_headView.setText("刷新中");
//                    if (listener!=null){
//                        if (!isRefreshing){
//                            isRefreshing=true;
//                            listener.onReresh();
//                        }
//                    }
//                }else {
//                    mScroll.startScroll(0,getScrollY(),0,distance,300);
//                    handler.post(new AnnimationTask());
//                }
//                }else{//正在刷新
//
//                }
//
//
//                break;
//        }
//        mLastY=currntY;
//        return true;
//    }

//    @Override
//    public void scrollTo(int x, int y) {
//        if (y>0&&y>=headView.getMeasuredHeight()){
//           y= headView.getMeasuredHeight();
//        }
//        super.scrollTo(x, y);
//    }

    @Override
    protected void onFinishInflate() {
        initView();
        super.onFinishInflate();
    }
    class AnnimationTask implements Runnable{

        @Override
        public void run() {
            if (mScroll.computeScrollOffset()){
                scrollTo(mScroll.getCurrX(),mScroll.getCurrY());
                post(this);
                if (getScrollY()==0){
                    isRefreshing=false;
                }
            }
        }

    }
    //刷新接口
    public interface RefreshListener{
        void onReresh();
    }
    public void setOnRefreshListener(RefreshListener RefreshListener){
        this.listener=RefreshListener;
    }
    //刷新结束
    public void OnRefreshComplete(){

        int distance=getScrollY()+headView.getMeasuredHeight();
        if(distance==0){
            distance=headView.getMeasuredHeight();
        }
        mScroll.startScroll(0,getScrollY(),0,distance,500);
        handler.post(new AnnimationTask());

    }
}
