package joke.ike.com.pulltorefreshlayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;

import org.w3c.dom.ls.LSInput;

/**
 * Created by ike on 2016/12/29.
 * 下拉刷新控件
 */
public class PullToRefreshLayout extends LinearLayout implements ValueAnimator.AnimatorUpdateListener {
    private String Tag = "PullToRefreshLayout";
    private View headView;
    private TextView tv_headView;
    private Scroller mScroll;
    private ListView contentView;
    private RefreshListener listener;
    private boolean isRefreshing;
    //lastPos 是最近一次抬手或者动画完成时的ChildView的偏移量
    private float LastPos = 0;
    //当前位置偏移量
    private float offsetY;
    private boolean mHasSendCancel = false;
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
        initAnimator();
    }
    private void initView() {
        contentView= (ListView) findViewById(R.id.content);
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
        Boolean other = Mime(ev);
        if (other!=null){
            return other;
        }

        return super.dispatchTouchEvent(ev);
    }
    private float startY;
    float my_start_Y=0;
    @Nullable
    private Boolean Mime(MotionEvent ev) {
       // int y = (int) ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                my_start_Y=ev.getY();
                super.dispatchTouchEvent(ev);
                Log.e(Tag,"down事件");
                return true;
            case MotionEvent.ACTION_MOVE:
                float curr_Y=ev.getY();
                //子view还可可以向上滑动：未到达顶部,不拦截事件，继续向子view分发
                if (canChildScrollUp()){
                   my_start_Y=curr_Y;
                  break;
                }else {
                    float dy=curr_Y-my_start_Y;
                    //子view已经到达顶部，不可滑动：这是拦截事件不在将事件分发到子view，由父亲布局相应事件，拦截事件
                    //dy:当前滑动距离
                    float newOffset=LastPos+dy/2;//当前的偏移量
                   // Log.e(Tag,"newOffset:"+newOffset);
                    //newOffset=0:headView头布局刚好被隐藏
                    //newOffset<0:父布局交付滑动事件，子孩子响应滑动事件
                   // Log.e(Tag,"newOffset:"+newOffset+",dy:"+dy);
                    if (newOffset<0){
                        if (!mHasSendCancel) {
                            return super.dispatchTouchEvent(ev);
                        }else {
                            LastPos=0;
                            move(0);
                            //重置move事件为down事件，重新进行事件的分发
                          //  MotionEvent downEvent = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime() + ViewConfiguration.getLongPressTimeout(), MotionEvent.ACTION_DOWN, ev.getX(), ev.getY(), ev.getMetaState());

                            ev.setAction(MotionEvent.ACTION_DOWN);
                            MotionEvent downEvent = MotionEvent.obtain(ev);
                            mHasSendCancel = false;
                            return super.dispatchTouchEvent(downEvent);
                        }
                    }
                    if (!mHasSendCancel) {
                        //down事件被下层接收，造成下层控件显示按下效果（比如listview按下时Item颜色加深），如果此后要拦截move事件，就发一个cancel事件让下层view取消按下的效果
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent cancelEvent = MotionEvent.obtain(ev);
                        super.dispatchTouchEvent(cancelEvent);
                        mHasSendCancel = true;
                    }
                    move(newOffset);

                    return true;
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                LastPos= offsetY;
                //当前位移距离==0，即是头布局已经隐藏，则需要将事件分发下去，有子view处理相关的事件
                if (offsetY<=0){
                    break;
                }
                //如果位移距离没有超过头布局的高度
                if (offsetY<headView.getMeasuredHeight()){
                        //直接返回头部
                        backToTop();
                }else if (offsetY>=headView.getMeasuredHeight()){
                        backToRefresh();
                    if (!isRefreshing){
                        isRefreshing=true;
                        if (listener!=null){
                            listener.onReresh();
                        }
                    }
                }

            break;
        }

        return super.dispatchTouchEvent(ev);
    }
    private ValueAnimator mBackToTop;
    private ValueAnimator mBackToRefreshing;
    /**
     * 将布局而重新摆回初始化的位置
     */
    private void backToTop() {
        mBackToTop.setFloatValues(offsetY,0);
        mBackToTop.start();
    }

    /**
     * 回到刷新的位置
     */
    private void backToRefresh(){
        Log.e(Tag,"回到刷新位置");
        mBackToRefreshing.setFloatValues(offsetY,headView.getMeasuredHeight()*1.0f);
        mBackToRefreshing.start();

    }


    /**
     * 初始化各个动画参数
     */
    public void initAnimator(){
        //返回顶部的动画
        mBackToTop=new ValueAnimator();
        mBackToTop.setDuration(500);
        mBackToTop.addUpdateListener(this);
        //返回刷新位置的动画
        mBackToRefreshing=new ValueAnimator();
        mBackToRefreshing.setDuration(500);
        mBackToRefreshing.addUpdateListener(this);
    }
    /**
     * 移动布局
     */
    private void move(float to) {
        //当前偏移量与目的位置的距离的差值
        int change= (int) (to-offsetY);
        if (change==0){
            return;
        }
        headView.offsetTopAndBottom(change);
        contentView.offsetTopAndBottom(change);
        invalidate();
        offsetY=offsetY+change;
    }
    @Override
    protected void onFinishInflate() {
        initView();
        super.onFinishInflate();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float dy = (float) animation.getAnimatedValue();
        move(dy);
        LastPos=dy;
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
        backToTop();
        isRefreshing=false;
    }
    /**
     * 判断子控件能否向下拉,主要代码来自{@link SwipeRefreshLayout#canChildScrollUp()}
     *
     * @return 能则返回true
     */
    private boolean canChildScrollUp()
    {
        //如果用户自己实现判断逻辑，则以用户的逻辑为准
        if (contentView == null)
            return true;

        if (android.os.Build.VERSION.SDK_INT < 14)
        {
            if (contentView instanceof AbsListView)
            {
                final AbsListView absListView = (AbsListView) contentView;
                return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else
            {
                return ViewCompat.canScrollVertically(contentView, -1) || contentView.getScrollY() > 0;
            }
        } else
        {
            return contentView.canScrollVertically(-1);
        }
    }
}
