package com.devilyang.musicstation.fragment;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.devilyang.musicstation.DetailActivity;
import com.devilyang.musicstation.R;
import com.devilyang.musicstation.adapter.MVListAdapter;
import com.devilyang.musicstation.bean.MVListBean;
import com.devilyang.musicstation.bean.MVListBean.Videos;
import com.devilyang.musicstation.cache.CacheManager;
import com.devilyang.musicstation.net.LogUtil;
import com.devilyang.musicstation.pullrefresh.PullToRefreshBase;
import com.devilyang.musicstation.pullrefresh.PullToRefreshBase.Mode;
import com.devilyang.musicstation.pullrefresh.PullToRefreshListView;
import com.devilyang.musicstation.swinginadapters.SwingBottomInAnimationAdapter;
import com.devilyang.musicstation.util.URLProviderUtil;
import com.devilyang.musicstation.util.Util;

public class MVPageFragment extends BaseFragment{
	
	private static final int SIZE = 25;
	private View rootView;
	private PullToRefreshListView mPullRefreshListView;
	private ProgressBar progressBar;
	private TextView failTips;
	private String areaCode;
	private ArrayList<Videos> videosList = new ArrayList<MVListBean.Videos>();
	private MVListBean bean;
	private MVListAdapter adapter;
	private int offset=0;
	/**当有缓存的时候，延时去加载500毫秒去加载数据，避免下拉刷新提示一直存在，暂未找到好办法*/
	private Handler handler;
	private JSONObject jsonObject;
	private Runnable runnable =  new Runnable() {
		
		@Override
		public void run() {
			parseResponse(jsonObject);
		}
	};
	/**当有缓存的时候，延时去加载500毫秒去加载数据，避免下拉刷新提示一直存在，暂未找到好办法*/
	public static MVPageFragment newInstance(String areaCode) {
		MVPageFragment fragment = new MVPageFragment();
		fragment.areaCode = areaCode;
		return fragment;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		LogUtil.d("MVPageFragment", "MVPageFragment onCreateView() = "+areaCode);
		if(rootView == null){
			LogUtil.d("MVPageFragment", "MVPageFragment rootView == null ");
			rootView=inflater.inflate(R.layout.pull_to_refresh_listview, null, false);
			findView(rootView);
			handler = new Handler();
			startLoadData(offset,SIZE);
		}
		LogUtil.d("MVPageFragment", "MVPageFragment areaCode = "+areaCode+" offset = "+offset);
		//缓存的rootView需要判断是否已经被加过parent， 如果有parent需要从parent删除，要不然会发生这个rootview已经有parent的错误。  
        ViewGroup parent = (ViewGroup) rootView.getParent();  
        if (parent != null) {  
            parent.removeView(rootView);  
        }   
		return rootView;
	}
	@Override
	public void onResume() {
		super.onResume();
		LogUtil.d("MVPageFragment", "MVPageFragment onResume() = "+areaCode);
	}
	@Override
	public void onPause() {
		super.onPause();
		LogUtil.d("MVPageFragment", "MVPageFragment onPause() = "+areaCode);
	}
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		LogUtil.d("MVPageFragment", "MVPageFragment onDestroyView() = "+areaCode);
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	private void startLoadData(int offset,int size) {
		if(bean!=null){
			if(offset>=bean.getTotalCount()-1){
				mPullRefreshListView.setMode(Mode.DISABLED);
				return;
			}
		}
		mPullRefreshListView.setMode(Mode.PULL_FROM_END);
		String url =  URLProviderUtil.getMVListUrl(areaCode, offset,
				size);
		jsonObject = CacheManager.getInstance().getACache().getAsJSONObject(url);
		if (jsonObject == null) {
			executeRequest(new JsonObjectRequest(Method.GET, url, null,
					responseListener(), errorSponseListener()),
					"MVPageFragment");
		}else{
			LogUtil.e("error", "有缓存数据啦。。。更新。。。");
			handler.postDelayed(runnable, 500);
		}
		
	}
	private void findView(View v){
		failTips = (TextView)v.findViewById(R.id.failed_tips);
		mPullRefreshListView = (PullToRefreshListView)v.findViewById(R.id.pull_refresh_list);
		progressBar = (ProgressBar)v.findViewById(R.id.mv_root_progress);
		// Set a listener to be invoked when the list should be refreshed.
		mPullRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {

			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				LogUtil.d("MVPageFragment", "MVPageFragment onPullDownToRefresh");
				String label = DateUtils.formatDateTime(getActivity(),
						System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME
								| DateUtils.FORMAT_SHOW_DATE
								| DateUtils.FORMAT_ABBREV_ALL);
				// Update the LastUpdatedLabel
				refreshView.getLoadingLayoutProxy()
						.setLastUpdatedLabel(label);
				// Do work to refresh the list here.
				if(!mPullRefreshListView.isRefreshing()){
					
				}
			}
			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
				LogUtil.d("MVPageFragment", "MVPageFragment onPullUpToRefresh");
				String label = DateUtils.formatDateTime(getActivity(),
						System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME
								| DateUtils.FORMAT_SHOW_DATE
								| DateUtils.FORMAT_ABBREV_ALL);

				// Update the LastUpdatedLabel
				refreshView.getLoadingLayoutProxy()
						.setLastUpdatedLabel(label);
				// Do work to refresh the list here.
				LogUtil.d("MVPageFragment", "MVPageFragment mPullRefreshListView.isRefreshing() = "+mPullRefreshListView.isRefreshing());
				startLoadData(offset, SIZE);
			}
		});
		ListView actualListView = mPullRefreshListView.getRefreshableView();
		
		mPullRefreshListView.setMode(Mode.PULL_FROM_END);
		mPullRefreshListView.setScrollingWhileRefreshingEnabled(false);
		adapter = new MVListAdapter(videosList, getActivity());
//		actualListView.setAdapter(adapter);
		SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(
				adapter);
		swingBottomInAnimationAdapter.setInitialDelayMillis(300);
		swingBottomInAnimationAdapter.setAbsListView(actualListView);

		actualListView.setAdapter(swingBottomInAnimationAdapter);
		actualListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent();
				intent.putExtra("id", videosList.get(position-1).getId());
				intent.putExtra("isRelativeVideo", true);
				intent.setClass(getActivity(), DetailActivity.class);
				getActivity().startActivity(intent);
			}
		});
		failTips.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startLoadData(offset, SIZE);
				failTips.setVisibility(View.GONE);
				progressBar.setVisibility(View.VISIBLE);
			}
		});
	}
	private void updateUI(){
		mPullRefreshListView.onRefreshComplete();
		offset +=SIZE; 
		adapter.notifyDataSetChanged();
	}
	@Override
	public Listener<JSONObject> responseListener() {
		return new Response.Listener<JSONObject>() {

			@Override
			public void onResponse(JSONObject response) {
//				LogUtil.d("MVPageFragment", "MVPageFragment response = "+response.toString());
				CacheManager.getInstance().getACache().put(URLProviderUtil.getMVListUrl(areaCode, offset,
				SIZE), response,Util.SAVE_TIME);
				parseResponse(response);
			}

		};
	}
	@Override
	public ErrorListener errorSponseListener() {
		return new Response.ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) {
				mPullRefreshListView.onRefreshComplete();
				if(offset==0){
					failTips.setVisibility(View.VISIBLE);
					progressBar.setVisibility(View.GONE);
				}else{
					if(getActivity()!=null){
						Toast.makeText(getActivity(), R.string.net_error_tips, Toast.LENGTH_SHORT).show();
					}
					
				}
//				LogUtil.d("MVPageFragment", "MVPageFragment onErrorResponse = "+error.getLocalizedMessage());
			}
		};
	}
	private void parseResponse(JSONObject response) {
		progressBar.setVisibility(View.GONE);
		LogUtil.e("error", "onRefreshComplete。。。");
		mPullRefreshListView.onRefreshComplete();
		try {
			bean = new MVListBean(response);
			videosList.addAll(bean.getVideosList());
			updateUI();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
