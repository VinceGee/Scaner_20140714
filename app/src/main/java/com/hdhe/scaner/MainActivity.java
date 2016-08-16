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


	private ListView mListview;  			//ListView ������ʾ����
	private List<Map<String, Object>>  mlist;  
	private List<Goods> list_goods;  		//�������
	private Button scan_btn;   				//ɨ�谴ť
	private Button exit_btn;				//�˳���ť
	private CheckBox per_100ms; 			//ÿ100����ɨһ�εĸ�ѡ��
	private String filepath ; 				//Excel����·��
	private String cmd = "scan";
	
	//���ڱ���ɨ�����
	private FileOutputStream fos;
	
	private List<Goods>  listFromExcel;		//�洢��Excel���ȡ��������
	
	private MyBroadcast myBroad;  //�㲥������
	
	private String activity = "com.hdhe.scanner.MainActivity";
	public String TAG = "MainActivity";  //Debug
	private MediaPlayer mPlayer;  //ý�岥���ߣ����ڲ�����ʾ��
	private FunkeyListener receive; //���ܼ��㲥������
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		//��ȡHOME ��
		getWindow().addFlags(0x80000000);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//��ʼ��
		init();
		
		receive  = new FunkeyListener();
		//����ע�Ṧ�ܼ��㲥������
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
		list_goods = new ArrayList<Goods>();   //��ʼ��list
		
//		import_btn.setOnClickListener(MainActivity.this);
		scan_btn.setOnClickListener(MainActivity.this);
		exit_btn.setOnClickListener(MainActivity.this);
		//��ѡ�����
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
		
		// ע��㲥
		Log.e(TAG, "on start");
		myBroad = new MyBroadcast();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.hdhe.scanner.MainActivity");
		registerReceiver(myBroad, filter);
		Log.e(TAG, "register Receiver");
		//��������
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
		sendBroadcast(stopService);  //�������͹㲥,�����ֹͣ
		Log.e(TAG, "send stop");
		
		super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.scan_btn:
			Log.e(TAG, "send cmd by scan button");
			sendCmd();   //����ָ�����
			break;
		case R.id.exit_btn:
//			onDestroy();
			finish();
			break;
		default:
			break;
		}
	}
	
	
	//��ȡkeycode,��ʼɨ��
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		Log.e("aaaaaaaaaaaaaaaaaaaaaa", keyCode+"");
		if( keyCode == KeyEvent.KEYCODE_HOME){
			Log.e(TAG, "send cmd by voleme button");
			sendCmd();   //����ָ�����
			return true;
		}
		if(keyCode  == KeyEvent.KEYCODE_BACK){
			finish();
		}
		
		return super.onKeyDown(keyCode, event);
		
		
	}
	

	
	/**
	 * ����ָ��
	 */
	private void sendCmd() {
		// �������͹㲥������Ϊcom.example.scandemo.MainActivity
		Intent ac = new Intent();
		ac.setAction("com.hdhe.service.ScanService");
		ac.putExtra("activity", activity);
		sendBroadcast(ac);
		Log.e(TAG, "send broadcast");

		Intent sendToservice = new Intent(MainActivity.this, ScanService.class); // ���ڷ���ָ��
		sendToservice.putExtra("cmd", cmd);
		this.startService(sendToservice); // ����ָ��
	}
	
	
	/**
	 * 
	 * @param list   ԭ������
	 * @param barcode  ��ɨ�赽��һά��
	 * @return  ���ص��ǽ���ɨ���������ӵ�ԭ�е����ݲ����ڵ�һλ
	 */
	private List<Goods> sortAndadd(List<Goods> list, String barcode){
		Goods goods = new Goods();
		goods.setBarcode(barcode);
		int temp = 1;
		if(list == null || list.size() == 0){  //��һ���������
			goods.setCount(temp);
			list.add(goods);
			return list;
		}
		//ԭ���������Ѿ�������
		for(int i = 0; i < list.size(); i++){  //����ԭ��list
			if(barcode.equals(list.get(i).getBarcode())){ 
				temp = list.get(i).getCount() + temp;
				goods.setCount(temp);
				for(int j = i; j > 0 ; j--){
					list.set(j, list.get(j-1));  //�ƶ�����
				}
				list.set(0, goods);
				return list;
			}
		}
		//ԭ������������
		Goods lastgoods = list.get(list.size() - 1);  //�Ȱ����һλȡ����
		for(int j = list.size() - 1; j >= 0 ; j--){
			if(j == 0){
				goods.setCount(temp); 
				list.set(j, goods);   //��ɨ������ݷŵ�һ��
			}else{
				list.set(j, list.get(j - 1));
			}
			
		}
		list.add(lastgoods);
		return list;
	}
	
	//���ܰ����㲥����
	private class FunkeyListener extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			boolean defaultdown=false;
	        int keycode = intent.getIntExtra("keycode", 0);    
	        boolean keydown = intent.getBooleanExtra("keydown", defaultdown);      
	        Log.i("ServiceDemo", "receiver:keycode="+keycode+"keydown="+keydown); 
	        //����°���
	        if(keycode == 133 && keydown){
	        	sendCmd();
	        }
	        //�Ҳఴ��
	        if(keycode == 134 && keydown){
	        	sendCmd();
	        }
	        
	        if(keycode == 131 && keydown){
//	        	Toast.makeText(getApplicationContext(), "����F1����", 0).show();
	        }
	        
	        if(keycode == 132 && keydown){
//	        	Toast.makeText(getApplicationContext(), "����F2����", 0).show();
	        }
			
		}
		
	}	
	

	
	/**
	 *  �㲥������,���շ����͹��������ݣ�������UI
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
			String receivedata = intent.getStringExtra("result"); // ���񷵻ص�����
			if (receivedata != null) {
				Log.e(TAG  + "  receivedata", receivedata);
				mlist = new ArrayList<Map<String,Object>>(); 
				//�����ݽ�������
				list_goods = sortAndadd(list_goods, receivedata); 
				String  allcount = list_goods.get(0).getCount()+"";
				//д���̶����ļ���
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
				//ý�岥��
				mPlayer = MediaPlayer.create(MainActivity.this, R.raw.msg); 
				if(mPlayer.isPlaying()){
					return;
				}
				mPlayer.start();
//				Selection.setSelection(receive_data.getEditableText(), 0);  //�ù�걣������ǰ��
			}
		}

	}

}
