package joke.ike.com.pulltorefreshlayout;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private PullToRefreshLayout pullToRefreshLayout;
    private ListView lv;
    private Handler handler=new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lv= (ListView) findViewById(R.id.lv);
        pullToRefreshLayout= (PullToRefreshLayout) findViewById(R.id.activity_main);
        pullToRefreshLayout.setOnRefreshListener(new PullToRefreshLayout.RefreshListener() {
            @Override
            public void onReresh() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullToRefreshLayout.OnRefreshComplete();
                    }
                },3000);
            }
        });
        lv.setAdapter(new Adapter());
    }
    class  Adapter extends BaseAdapter{

        @Override
        public int getCount() {
            return 50;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView=new TextView(parent.getContext());
            textView.setText("我是内容");
            return textView;
        }
    }
}
