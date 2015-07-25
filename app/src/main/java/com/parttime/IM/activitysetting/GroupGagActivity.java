package com.parttime.IM.activitysetting;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.carson.constant.ConstantForSaveList;
import com.easemob.chat.EMGroupManager;
import com.easemob.chatuidemo.activity.BaseActivity;
import com.easemob.exceptions.EaseMobException;
import com.parttime.common.Image.ContactImageLoader;
import com.parttime.common.head.ActivityHead;
import com.parttime.constants.ActivityExtraAndKeys;
import com.parttime.pojo.BaseUser;
import com.qingmu.jianzhidaren.R;
import com.quark.utils.NetWorkCheck;
import com.quark.volley.VolleySington;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GroupGagActivity extends BaseActivity {

    private ActivityHead headView;
    private ListView listView;

    private UngagAdapter adapter = new UngagAdapter();

    private ArrayList<AdapterVO> adapterVOs = new ArrayList<>();

    private String groupId;

    protected RequestQueue queue = VolleySington.getInstance().getRequestQueue();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_gag);
        
        initView(); 
        
        bindData();
        
    }

    private void initView() {
        headView = new ActivityHead(this);
        headView.setCenterTxt1(R.string.group_not_allow_send_msg);
        listView = (ListView)findViewById(R.id.listView);

        listView.setAdapter(adapter);

        Intent intent = getIntent();
        groupId = intent.getStringExtra(ActivityExtraAndKeys.GroupSetting.GROUPID);

        new Thread(new Runnable() {

            public void run() {
                try {
                    final List<String> blockedList = EMGroupManager.getInstance()
                            .getBlockedUsers(groupId);
                    if (blockedList != null) {
                        Collections.sort(blockedList);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Map<String, BaseUser> userIdPictureMap = ConstantForSaveList.userIdUserCache;
                                for(String userId : blockedList) {
                                    AdapterVO vo = new AdapterVO();
                                    vo.userId = userId;
                                    if(userIdPictureMap != null){
                                        BaseUser baseUser = userIdPictureMap.get(userId);
                                        if(baseUser != null) {
                                            vo.picture = baseUser.picture;
                                            vo.name = baseUser.name;
                                        }
                                    }
                                    adapterVOs.add(vo);
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (EaseMobException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "获取失败,请检查网络或稍后再试", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void bindData() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除保存的UserId和picture
        ConstantForSaveList.userIdUserCache.clear();
    }

    class UngagAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return adapterVOs.size();
        }

        @Override
        public AdapterVO getItem(int position) {
            return adapterVOs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view ;
            ViewHolder holder ;
            if(convertView == null){
                view = getLayoutInflater().inflate(R.layout.activity_group_gag_item,parent, false);

                holder = new ViewHolder();
                holder.picture = (ImageView)view.findViewById(R.id.picture);
                holder.name = (TextView)view.findViewById(R.id.name);
                holder.unGag = (Button)view.findViewById(R.id.ungag);

                view.setTag(holder);

            }else{
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            bindItemData(holder, position);

            return view;
        }

        private void bindItemData(ViewHolder holder , int position) {
            AdapterVO itemVo = getItem(position);
            //设置头像
            String head = itemVo.picture;
            if (! TextUtils.isEmpty(head)) {
                // 默认加载本地图片
                ContactImageLoader.loadNativePhoto(String.valueOf(itemVo.userId),
                        head, holder.picture, queue);
            } else {
                holder.picture.setImageResource(R.drawable.default_avatar);
            }

            holder.name.setText(itemVo.name);
            holder.unGag.setTag(position);
            holder.unGag.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    if (! NetWorkCheck.isOpenNetwork(GroupGagActivity.this)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.no_net_tip), Toast.LENGTH_SHORT).show();
                    }
                    try {
                        int position = (int)v.getTag();
                        AdapterVO itemVo = getItem(position);
                        String tobeRemoveUser = itemVo.userId;
                        // 移出禁言
                        EMGroupManager.getInstance().unblockUser(groupId, tobeRemoveUser);
                        adapterVOs.remove(itemVo);
                        adapter.notifyDataSetChanged();
                    } catch (EaseMobException ignore) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.action_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
    }

    class AdapterVO{
        String userId;
        String name;
        String picture;
    }

    class ViewHolder{
        ImageView picture;
        TextView name;
        Button unGag;
    }

}