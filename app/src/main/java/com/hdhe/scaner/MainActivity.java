package com.hdhe.scaner;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.hdhe.entity.Goods;
import com.hdhe.service.ScanService;

public class MainActivity extends Activity implements OnClickListener{


	private ListView mListview;  			//ListView 用于显示数据
	private List<Map<String, Object>>  mlist;  
	private List<Goods> list_goods;  		//存放数据
	private Button scan_btn;   				//扫描按钮
	private Button exit_btn;				//退出按钮
	private CheckBox per_100ms; 			//每100毫秒扫一次的复选框
	private String filepath ; 				//Excel表格的路径
	private String cmd = "scan";
	
	//用于保存扫描次数
	private FileOutputStream fos;
	
	private List<Goods>  listFromExcel;		//存储从Excel表获取到的数据
	
	private MyBroadcast myBroad;  //广播接收者
	
	private String activity = "com.hdhe.scanner.MainActivity";
	public String TAG = "MainActivity";  //Debug
	private MediaPlayer mPlayer;  //媒体播放者，用于播放提示音
	private FunkeyListener receive; //功能键广播接收者
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		//截取HOME 键
		getWindow().addFlags(0x80000000);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//初始化
		init();
		
		receive  = new FunkeyListener();
		//代码注册功能键广播接收者
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.FUN_KEY");
		registerReceiver(receive, filter);
		
	}
	
	private void init(){
//		 receive_data = (EditText) findViewById(R.id.receive);
//		import_btn = (Button) findViewById(R.id.import_file_btn);
//		import_path = (EditText) findViewById(R.id.import_file_path_edt);
		mListview = (ListView) findViewById(R.id.data_list);   
		scan_btn = (Button) findViewById(R.id.scan_btn);
		exit_btn = (Button) findViewById(R.id.exit_btn);
		per_100ms = (CheckBox) findViewById(R.id.per_100ms);
		list_goods = new ArrayList<Goods>();   //初始化list
		
//		import_btn.setOnClickListener(MainActivity.this);
		scan_btn.setOnClickListener(MainActivity.this);
		exit_btn.setOnClickListener(MainActivity.this);
		//复选框监听
		per_100ms.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
					Log.e(TAG, "check box");
					cmd = "toscan100ms";
//					sendCmd();
				}else{
					cmd = "scan";
				}
				
				
			}
		});
		
		// 注册广播
		Log.e(TAG, "on start");
		myBroad = new MyBroadcast();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.hdhe.scanner.MainActivity");
		registerReceiver(myBroad, filter);
		Log.e(TAG, "register Receiver");
		//启动服务
		Intent start = new Intent(MainActivity.this, ScanService.class);
		MainActivity.this.startService(start);
		
		
	}
	
	
	
	@Override
		protected void onPause() {
			// TODO Auto-generated method stub
			super.onPause();
		}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
//		mPlayer.stop();
		unregisterReceiver(myBroad);
		Intent stopService = new Intent();
		stopService.setAction("com.hdhe.service.ScanService");
		stopService.putExtra("stopflag", true);
		sendBroadcast(stopService);  //给服务发送广播,令服务停止
		Log.e(TAG, "send stop");
		
		super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.scan_btn:
			Log.e(TAG, "send cmd by scan button");
			sendCmd();   //发送指令到服务
			break;
		case R.id.exit_btn:
//			onDestroy();
			finish();
			break;
		default:
			break;
		}
	}
	
	
	//获取keycode,开始扫描
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		Log.e("aaaaaaaaaaaaaaaaaaaaaa", keyCode+"");
		if( keyCode == KeyEvent.KEYCODE_HOME){
			Log.e(TAG, "send cmd by voleme button");
			sendCmd();   //发送指令到服务
			return true;
		}
		if(keyCode  == KeyEvent.KEYCODE_BACK){
			finish();
		}
		
		return super.onKeyDown(keyCode, event);
		
		
	}
	

	
	/**
	 * 发送指令
	 */
	private void sendCmd() {
		// 给服务发送广播，内容为com.example.scandemo.MainActivity
		Intent ac = new Intent();
		ac.setAction("com.hdhe.service.ScanService");
		ac.putExtra("activity", activity);
		sendBroadcast(ac);
		Log.e(TAG, "send broadcast");

		Intent sendToservice = new Intent(MainActivity.this, ScanService.class); // 用于发送指令
		sendToservice.putExtra("cmd", cmd);
		this.startService(sendToservice); // 发送指令
	}
	
	
	/**
	 * 
	 * @param list   原有数据
	 * @param barcode  新扫描到的一维码
	 * @return  返回的是将新扫描的数据添加到原有的数据并放在第一位
	 */
	private List<Goods> sortAndadd(List<Goods> list, String barcode){
		Goods goods = new Goods();
		goods.setBarcode(barcode);
		int temp = 1;
		if(list == null || list.size() == 0){  //第一次添加数据
			goods.setCount(temp);
			list.add(goods);
			return list;
		}
		//原有数据中已经有条码
		for(int i = 0; i < list.size(); i++){  //遍历原有list
			if(barcode.equals(list.get(i).getBarcode())){ 
				temp = list.get(i).getCount() + temp;
				goods.setCount(temp);
				for(int j = i; j > 0 ; j--){
					list.set(j, list.get(j-1));  //移动数据
				}
				list.set(0, goods);
				return list;
			}
		}
		//原有数据无条码
		Goods lastgoods = list.get(list.size() - 1);  //先把最后一位取出来
		for(int j = list.size() - 1; j >= 0 ; j--){
			if(j == 0){
				goods.setCount(temp); 
				list.set(j, goods);   //新扫描的数据放第一行
			}else{
				list.set(j, list.get(j - 1));
			}
			
		}
		list.add(lastgoods);
		return list;
	}
	
	//功能按键广播监听
	private class FunkeyListener extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			boolean defaultdown=false;
	        int keycode = intent.getIntExtra("keycode", 0);    
	        boolean keydown = intent.getBooleanExtra("keydown", defaultdown);      
	        Log.i("ServiceDemo", "receiver:keycode="+keycode+"keydown="+keydown); 
	        //左侧下按键
	        if(keycode == 133 && keydown){
	        	sendCmd();
	        }
	        //右侧按键
	        if(keycode == 134 && keydown){
	        	sendCmd();
	        }
	        
	        if(keycode == 131 && keydown){
//	        	Toast.makeText(getApplicationContext(), "这是F1按键", 0).show();
	        }
	        
	        if(keycode == 132 && keydown){
//	        	Toast.makeText(getApplicationContext(), "这是F2按键", 0).show();
	        }
			
		}
		
	}	
	

	
	/**
	 *  广播接收者,接收服务发送过来的数据，并更新UI
	 * @author Administrator
	 *
	 */
	private class MyBroadcast extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				fos = MainActivity.this.openFileOutput("count.txt", Context.MODE_PRIVATE);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			String receivedata = intent.getStringExtra("result"); // 服务返回的数据
			if (receivedata != null) {
				Log.e(TAG  + "  receivedata", receivedata);
				mlist = new ArrayList<Map<String,Object>>(); 
				//对数据进行排序
				list_goods = sortAndadd(list_goods, receivedata); 
				String  allcount = list_goods.get(0).getCount()+"";
				//写到固定的文件中
				try {
					fos.write(allcount.getBytes());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					fos.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				int count = 1;
				for(Goods goods:list_goods ){
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("barcodeID", count);
					map.put("barcode", goods.getBarcode());
					map.put("count", goods.getCount());
					count++;
					mlist.add(map);
				}
				
				mListview.setAdapter(new SimpleAdapter(MainActivity.this,
						mlist, R.layout.listview_item, new String[] {
						"barcodeID","barcode", "count" }, new int[] {
								R.id.barcodeID_item,R.id.barcode_item, R.id.count_item }));
				//媒体播放
				mPlayer = MediaPlayer.create(MainActivity.this, R.raw.msg); 
				if(mPlayer.isPlaying()){
					return;
				}
				mPlayer.start();
//				Selection.setSelection(receive_data.getEditableText(), 0);  //让光标保持在最前面
			}
		}

	}

}
