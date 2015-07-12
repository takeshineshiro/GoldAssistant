package com.parttime.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.carson.constant.ConstantForSaveList;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContact;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMConversation;
import com.easemob.chat.EMGroup;
import com.easemob.chat.EMGroupManager;
import com.easemob.chat.EMMessage;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.activity.AddContactActivity;
import com.easemob.chatuidemo.activity.ChatActivity;
import com.easemob.chatuidemo.activity.GroupsActivity;
import com.easemob.chatuidemo.activity.NewFriendsMsgActivity;
import com.easemob.chatuidemo.adapter.ChatAllHistoryAdapter;
import com.easemob.chatuidemo.adapter.ContactAdapter;
import com.easemob.chatuidemo.db.InviteMessgeDao;
import com.easemob.chatuidemo.db.UserDao;
import com.easemob.chatuidemo.domain.User;
import com.easemob.chatuidemo.widget.Sidebar;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.NetUtils;
import com.qingmu.jianzhidaren.R;
import com.quark.citylistview.CharacterParser;
import com.quark.common.JsonUtil;
import com.quark.common.ToastUtil;
import com.quark.common.Url;
import com.quark.jianzhidaren.ApplicationControl;
import com.quark.model.HuanxinUser;
import com.quark.quanzi.MyContactlistFragment;
import com.quark.quanzi.PinyinComparator_quanzhi;
import com.quark.quanzi.PinyinComparator_quanzhitwo;
import com.quark.volley.VolleySington;

public class QuanzhiFragment extends Fragment {
	private RequestQueue queue;
	private ViewPager viewPager; // viewpager
	private ArrayList<View> pageViews; // 2个viewpager 页面
	private ViewGroup buttonsLine;
	private TextView msgTv, contactsTv;// 消息、联系人
	private ImageView quanziShenqingHongdianImv;
	private View page1, page2; // 消息、联系人
	private boolean hidden;
	// 消息
	private InputMethodManager inputMethodManager;
	private ListView listView;
	private ChatAllHistoryAdapter adapter;
	private List<EMGroup> groups;
	private List<EMConversation> conversationList = null;
	private ImageView contact_hongdian_imv;// 右上角通讯录小红点图标
	// 通讯录、联系人
	private ContactAdapter contactAdapter;
	private List<User> contactList;
	private List<String> contactIds;
	private String contactIdsStr = "";
	private ListView contactlistView;
	private Sidebar sidebar;
	private List<String> blackList;
	ArrayList<HuanxinUser> usersNick = new ArrayList<HuanxinUser>();
	// =========转拼音========
	private CharacterParser characterParser;

	/**
	 * 根据拼音来排列ListView里面的数据类
	 */
	private PinyinComparator_quanzhi pinyinComparator;
	private PinyinComparator_quanzhitwo pinyinComparatorTwo;
	private RelativeLayout topRelativeLayout;// 上方

	public static QuanzhiFragment newInstance(String param1, String param2) {
		QuanzhiFragment fragment = new QuanzhiFragment();
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		queue = VolleySington.getInstance().getRequestQueue();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		pageViews = new ArrayList<View>();
		page1 = inflater.inflate(R.layout.fragment_conversation_history, null);
		page2 = inflater.inflate(R.layout.fragment_contact_list, null);
		pageViews.add(page1);
		pageViews.add(page2);
		buttonsLine = (ViewGroup) inflater.inflate(
				R.layout.fragment_quanzi_layout, null);

		SharedPreferences sp = getActivity().getSharedPreferences(
				"jrdr.setting", Context.MODE_PRIVATE);
		// top上方颜色变灰
		topRelativeLayout = (RelativeLayout) buttonsLine
				.findViewById(R.id.quanzi_top_relayout);

		// 消息、通讯录2个textview,申请好友的通知提示红点
		msgTv = (TextView) buttonsLine
				.findViewById(R.id.fragment_quanzi_msg_tv);
		contactsTv = (TextView) buttonsLine
				.findViewById(R.id.fragment_quanzi_contacts_tv);
		quanziShenqingHongdianImv = (ImageView) buttonsLine
				.findViewById(R.id.quanzi_shenqing_hongdian_imv);
		topRelativeLayout.setBackgroundColor(getResources().getColor(
				R.color.guanli_common_color));
		msgTv.setTextColor(getActivity().getResources().getColor(
				R.color.guanli_common_color));
		msgTv.setOnClickListener(new GuideButtonClickListener(0));
		contactsTv.setOnClickListener(new GuideButtonClickListener(1));
		viewPager = (ViewPager) buttonsLine.findViewById(R.id.guidePages);
		viewPager.setAdapter(new GuidePageAdapter());
		viewPager.setOnPageChangeListener(new GuidePageChangeListener());
		return buttonsLine;
	}

	/**
	 * 设置消息、通讯录背景颜色
	 * 
	 */
	private void setBackStatus(int positon) {
		// position 0 选中消息 1 选中通讯录
		if (positon == 0) {
			msgTv.setBackgroundDrawable(getActivity().getResources()
					.getDrawable(R.drawable.quanzi_btn_bar_left_on));

			msgTv.setTextColor(getActivity().getResources().getColor(
					R.color.guanli_common_color));

			contactsTv.setBackgroundDrawable(getActivity().getResources()
					.getDrawable(R.drawable.quanzi_btn_bar_right_off));
			contactsTv.setTextColor(getActivity().getResources().getColor(
					R.color.body_color));
		} else if (positon == 1) {
			msgTv.setBackgroundDrawable(getActivity().getResources()
					.getDrawable(R.drawable.quanzi_btn_bar_left_off));
			msgTv.setTextColor(getActivity().getResources().getColor(
					R.color.body_color));
			contactsTv.setBackgroundDrawable(getActivity().getResources()
					.getDrawable(R.drawable.quanzi_btn_bar_right_on));
			contactsTv.setTextColor(getActivity().getResources().getColor(
					R.color.guanli_common_color));

		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.getBoolean("isConflict", false))
			return;
		inputMethodManager = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		// ****************消息记录**************
		// 进入联系人列表
		ImageView contactsView = (ImageView) page1
				.findViewById(R.id.contactsView);
		contactsView.setOnClickListener(toContextView);
		// 联系人小红点
		contact_hongdian_imv = (ImageView) page1
				.findViewById(R.id.contacts_hongdian_imv);
		conversationList = loadConversationsWithRecentChat();
		listView = (ListView) page1.findViewById(R.id.list);
		adapter = new ChatAllHistoryAdapter(getActivity(), 1, conversationList);
		// 设置adapter
		listView.setAdapter(adapter);

		groups = EMGroupManager.getInstance().getAllGroups();

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				EMConversation conversation = adapter.getItem(position);
				String username = conversation.getUserName();
				if (username.equals(ApplicationControl.getInstance()
						.getUserName()))
					Toast.makeText(getActivity(), "不能和自己聊天", 0).show();
				else {
					// 进入聊天页面
					Intent intent = new Intent(getActivity(),
							ChatActivity.class);
					EMContact emContact = null;
					groups = EMGroupManager.getInstance().getAllGroups();
					for (EMGroup group : groups) {
						if (group.getGroupId().equals(username)) {
							emContact = group;
							break;
						}
					}
					if (emContact != null && emContact instanceof EMGroup) {
						// it is group chat
						intent.putExtra("chatType", ChatActivity.CHATTYPE_GROUP);
						intent.putExtra("groupId",
								((EMGroup) emContact).getGroupId());
					} else {
						// it is single chat
						intent.putExtra("userId", username);
					}
					startActivity(intent);
				}
			}
		});
		// 注册上下文菜单
		registerForContextMenu(listView);

		listView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 隐藏软键盘
				hideSoftKeyboard();
				return false;
			}
		});

		// ******************通讯录、联系人****************************
		// 实例化汉字转拼音类 加
		characterParser = CharacterParser.getInstance();
		pinyinComparator = new PinyinComparator_quanzhi();
		pinyinComparatorTwo = new PinyinComparator_quanzhitwo();
		contactlistView = (ListView) page2.findViewById(R.id.list);
		sidebar = (Sidebar) page2.findViewById(R.id.sidebar);
		sidebar.setListView(contactlistView);
		// 黑名单列表
		blackList = EMContactManager.getInstance().getBlackListUsernames();
		contactList = new ArrayList<User>();
		contactIds = new ArrayList<String>();
		ImageView addContactView = (ImageView) buttonsLine
				.findViewById(R.id.iv_new_contact);
		// 进入添加好友页
		addContactView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(),
						AddContactActivity.class));
			}
		});
		registerForContextMenu(contactlistView);

	}

	/**
	 * viewpager适配器
	 */
	class GuidePageAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return pageViews.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public int getItemPosition(Object object) {
			return super.getItemPosition(object);
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(pageViews.get(arg1));
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(pageViews.get(arg1));
			return pageViews.get(arg1);
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {

		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {

		}

		@Override
		public void finishUpdate(View arg0) {

		}
	}

	/**
	 * viewpager改变
	 */
	@SuppressLint("ResourceAsColor")
	class GuidePageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int arg0) {
			setBackStatus(arg0);
		}
	}

	/**
	 * button监听
	 */
	class GuideButtonClickListener implements OnClickListener {
		private int index = 0;

		public GuideButtonClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			viewPager.setCurrentItem(index, true);
		}
	}

	// *********************消息记录**************************

	/**
	 * 前往联系人
	 */
	OnClickListener toContextView = new OnClickListener() {

		@Override
		public void onClick(View v) {

			Intent intent = new Intent();
			intent.setClass(getActivity(), MyContactlistFragment.class);
			startActivity(intent);

		}
	};

	void hideSoftKeyboard() {
		if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
			if (getActivity().getCurrentFocus() != null)
				inputMethodManager.hideSoftInputFromWindow(getActivity()
						.getCurrentFocus().getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v == listView) {
			getActivity().getMenuInflater()
					.inflate(R.menu.delete_message, menu);
		} else if (v == contactlistView) {
			// 长按前两个不弹menu
			if (((AdapterContextMenuInfo) menuInfo).position >= 2) {
				getActivity().getMenuInflater().inflate(
						R.menu.context_contact_list, menu);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.delete_message) {
			EMConversation tobeDeleteCons = adapter
					.getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
			// 删除此会话
			EMChatManager.getInstance().deleteConversation(
					tobeDeleteCons.getUserName(), tobeDeleteCons.isGroup());
			InviteMessgeDao inviteMessgeDao = new InviteMessgeDao(getActivity());
			inviteMessgeDao.deleteMessage(tobeDeleteCons.getUserName());
			adapter.remove(tobeDeleteCons);
			adapter.notifyDataSetChanged();
			// 长按聊天记录item,删除聊天记录
			// 更新未读消息
			if (MainTabActivity.isForeground) {
				((MainTabActivity) getActivity()).update_unread_msg();
			}

			return true;
		} else if (item.getItemId() == R.id.delete_contact) {
			// User tobeDeleteUser = contactAdapter
			// .getItem(((AdapterContextMenuInfo) item.getMenuInfo()).position);
			// 删除此联系人
			// deleteContact(tobeDeleteUser);
			// 删除相关的邀请消息
			// InviteMessgeDao dao = new InviteMessgeDao(getActivity());
			// dao.deleteMessage(tobeDeleteUser.getUsername());
			// 方法二
			HuanxinUser user = usersNick.get(((AdapterContextMenuInfo) item
					.getMenuInfo()).position);
			deleteContact(user.getUid());
			InviteMessgeDao dao = new InviteMessgeDao(getActivity());
			dao.deleteMessage(user.getUid());
			return true;
		} else if (item.getItemId() == R.id.add_to_blacklist) {
			// User user = contactAdapter.getItem(((AdapterContextMenuInfo) item
			// .getMenuInfo()).position);
			// moveToBlacklist(user.getUsername());
			// 方法二
			HuanxinUser user = usersNick.get(((AdapterContextMenuInfo) item
					.getMenuInfo()).position);
			moveToBlacklist(user.getUid());
			return true;
		}
		return super.onContextItemSelected(item);
	}

	/**
	 * 刷新页面
	 */
	public void refresh() {
		if (conversationList != null) {
			conversationList.clear();
			List<EMConversation> sd = loadConversationsWithRecentChat();
			conversationList.addAll(sd);
			adapter.notifyDataSetChanged();
		}
	}

	/**
	 * 获取所有会话
	 * 
	 * @param context
	 * @return
	 */
	private List<EMConversation> loadConversationsWithRecentChat() {
		// 获取所有会话，包括陌生人
		Hashtable<String, EMConversation> conversations = EMChatManager
				.getInstance().getAllConversations();
		List<EMConversation> list = new ArrayList<EMConversation>();
		// 过滤掉messages seize为0的conversation
		for (EMConversation conversation : conversations.values()) {
			if (conversation.getAllMessages().size() != 0)
				list.add(conversation);
		}
		// 排序
		sortConversationByLastChatTime(list);
		return list;
	}

	/**
	 * 根据最后一条消息的时间排序
	 * 
	 * @param usernames
	 */
	private void sortConversationByLastChatTime(
			List<EMConversation> conversationList) {
		Collections.sort(conversationList, new Comparator<EMConversation>() {
			@Override
			public int compare(final EMConversation con1,
					final EMConversation con2) {
				EMMessage con2LastMessage = con2.getLastMessage();
				EMMessage con1LastMessage = con1.getLastMessage();
				if (con2LastMessage.getMsgTime() == con1LastMessage
						.getMsgTime()) {
					return 0;
				} else if (con2LastMessage.getMsgTime() > con1LastMessage
						.getMsgTime()) {
					return 1;
				} else {
					return -1;
				}
			}

		});
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);

		this.hidden = hidden;
		if (!hidden) {
			refresh();
		}
	}

	/**
	 * 获取未读申请与通知消息
	 * 
	 * @return
	 */
	private int getUnreadNotionCountTotal() {
		int unreadAddressCountTotal = 0;
		if (ApplicationControl.getInstance().getContactList()
				.get(Constant.NEW_FRIENDS_USERNAME) != null)
			unreadAddressCountTotal = ApplicationControl.getInstance()
					.getContactList().get(Constant.NEW_FRIENDS_USERNAME)
					.getUnreadMsgCount();
		return unreadAddressCountTotal;
	}

	/**
	 * 更新右上角通讯录红点 carson
	 */
	private void update_unread_notication() {

		int carson_unread_notion_count = getUnreadNotionCountTotal();//

		if (carson_unread_notion_count > 0) {
			quanziShenqingHongdianImv.setVisibility(View.VISIBLE);
		} else {
			quanziShenqingHongdianImv.setVisibility(View.GONE);
		}
	}

	// **********************消息记录end***********************************

	// **********************通讯录、联系人************************************

	// 刷新ui
	// public void ContactRefresh() {
	// try {
	// // 可能会在子线程中调到这方法
	// getActivity().runOnUiThread(new Runnable() {
	// public void run() {
	// getContactList();
	// contactAdapter.notifyDataSetChanged();
	// }
	// });
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public void dealdd() {
		// 保留缓存
		// usersNick.clear();
		// 获取设置contactlist
		// 获取头像及昵称
		getContactList();

		if (contactIds.size() > 0) {
			contactIdsStr = "{";
			for (int i = 0; i < contactIds.size(); i++) {
				contactIdsStr += contactIds.get(i) + "、";
			}
			contactIdsStr = contactIdsStr.substring(0,
					contactIdsStr.length() - 1) + "}";

			if (contactIdsStr.equals("{}")) {
				filledData();
			} else {
				// filledData();
				if (ConstantForSaveList.usersNick != null
						&& ConstantForSaveList.usersNick.size() > 2) {
					// 去除前2个申请与通知、群聊
					ConstantForSaveList.usersNick.remove(0);
					ConstantForSaveList.usersNick.remove(0);
					if (ConstantForSaveList.usersNick.size() == contactList
							.size()) {
						// 联系人未更改保留缓存
						usersNick = ConstantForSaveList.usersNick;
						filledData();
					} else {
						getNick();
					}
				} else {
					getNick();
				}
			}
		} else {
			filledData();
		}
	}

	/**
	 * 获取联系人列表，并过滤掉黑名单和排序
	 */

	private void getContactList() {
		contactList.clear();
		contactIds.clear();
		// UserDao ud = new UserDao(getActivity());
		// ApplicationControl.getInstance().setContactList(ud.getContactList());
		// 获取本地好友列表
		Map<String, User> users = ApplicationControl.getInstance()
				.getContactList();
		Iterator<Entry<String, User>> iterator = users.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, User> entry = iterator.next();
			if (!entry.getKey().equals(Constant.NEW_FRIENDS_USERNAME)
					&& !entry.getKey().equals(Constant.GROUP_USERNAME)
					&& !blackList.contains(entry.getKey())) {
				// 这里有bug，会有好友列表有uid,没有名字的情况
				if (!entry.getKey().equals("jianzhidaren")) {
					// userName ==nick 都是u661或者c221之类的
					// head 是u或者c
					contactList.add(entry.getValue());
				}
			}
		}
		for (int i = 0; i < contactList.size(); i++) {
			contactIds.add(contactList.get(i).getUsername());
		}

	}

	/**
	 * 删除联系人
	 * 
	 * @param toDeleteUser
	 */
	public void deleteContact(final User tobeDeleteUser) {
		final ProgressDialog pd = new ProgressDialog(getActivity());
		pd.setMessage("正在删除...");
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					EMContactManager.getInstance().deleteContact(
							tobeDeleteUser.getUsername());
					// 删除db和内存中此用户的数据
					UserDao dao = new UserDao(getActivity());
					dao.deleteContact(tobeDeleteUser.getUsername());
					ApplicationControl.getInstance().getContactList()
							.remove(tobeDeleteUser.getUsername());
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							contactAdapter.remove(tobeDeleteUser);
							contactAdapter.notifyDataSetChanged();
							dealdd();// 长按删除联系人要刷新当前页面

						}
					});
				} catch (final Exception e) {

				}
			}
		}).start();
	}

	/**
	 * 删除联系人
	 * 
	 * @param toDeleteUser
	 */
	public void deleteContact(final String uid) {
		final ProgressDialog pd = new ProgressDialog(getActivity());
		pd.setMessage("正在删除...");
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					EMContactManager.getInstance().deleteContact(uid);
					// 删除db和内存中此用户的数据
					UserDao dao = new UserDao(getActivity());
					dao.deleteContact(uid);
					ApplicationControl.getInstance().getContactList()
							.remove(uid);
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							if (contactList != null && contactList.size() > 0) {
								for (User u : contactList) {
									if (u.getUsername().equals(uid)) {
										contactAdapter.remove(u);
										break;
									}
								}
								contactAdapter.notifyDataSetChanged();
							}
							dealdd();// 长按删除联系人要刷新当前页面

						}
					});
				} catch (final Exception e) {

				}
			}
		}).start();
	}

	/**
	 * 把user移入到黑名单
	 */
	private void moveToBlacklist(final String username) {
		final ProgressDialog pd = new ProgressDialog(getActivity());
		pd.setMessage("正在移入黑名单...");
		pd.setCanceledOnTouchOutside(false);
		pd.show();
		new Thread(new Runnable() {
			public void run() {
				try {
					// 加入到黑名单
					EMContactManager.getInstance().addUserToBlackList(username,
							false);
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							ToastUtil.showShortToast("移入黑名单成功");
							refresh();
						}
					});
				} catch (EaseMobException e) {
					e.printStackTrace();
					getActivity().runOnUiThread(new Runnable() {
						public void run() {
							pd.dismiss();
							ToastUtil.showShortToast("移入黑名单失败");
						}
					});
				}
			}
		}).start();
	}

	public void getNick() {
		// showWait(true);
		StringRequest request = new StringRequest(Request.Method.POST,
				Url.HUANXIN_avatars_pic, new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						// showWait(false);
						try {
							JSONObject js = new JSONObject(response);
							JSONArray jss = js.getJSONArray("avatars");
							usersNick.clear();
							for (int i = 0; i < jss.length(); i++) {
								HuanxinUser us = (HuanxinUser) JsonUtil
										.jsonToBean(jss.getJSONObject(i),
												HuanxinUser.class);
								if (us.getName() == null
										|| us.getName().equals("")) {
									us.setName("未知好友");
								}
								usersNick.add(us);

							}
							if (usersNick.size() > 0) {
								ConstantForSaveList.usersNick = usersNick;// 保存缓存

							}
							filledData(); // 转化拼音

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						// showWait(false);
					}
				}) {
			@Override
			protected Map<String, String> getParams() throws AuthFailureError {

				Map<String, String> map = new HashMap<String, String>();
				map.put("user_ids", contactIdsStr);
				return map;
			}
		};
		queue.add(request);
		request.setRetryPolicy(new DefaultRetryPolicy(
				ConstantForSaveList.DEFAULTRETRYTIME * 1000, 1, 1.0f));

	}

	public void setlist() {
		// 设置adapter
		// 快速点击可能出现getActivity()为空
		if (getActivity() == null)
			return;
		contactAdapter = new ContactAdapter(getActivity(),
				R.layout.row_contact, contactList, sidebar, usersNick);
		contactlistView.setAdapter(contactAdapter);
		contactlistView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (NetUtils.hasNetwork(getActivity())) {
					User u = contactAdapter.getItem(position);
					if (u == null) {
						return;
					}
					String username = u.getUsername();
					if (username == null) {
						return;
					}
					if (Constant.NEW_FRIENDS_USERNAME.equals(username)) {
						// 进入申请与通知页面
						User user = ApplicationControl.getInstance()
								.getContactList()
								.get(Constant.NEW_FRIENDS_USERNAME);
						user.setUnreadMsgCount(0);
						startActivity(new Intent(getActivity(),
								NewFriendsMsgActivity.class));
					} else if (Constant.GROUP_USERNAME.equals(username)) {
						// 进入群聊列表页面
						Intent intent = new Intent(getActivity(),
								GroupsActivity.class);
						intent.putExtra("isFromShare", false);
						startActivity(intent);
					} else {

						// 中直接进入聊天页面，实际一般是进入用户详情页
						// startActivity(new Intent(getActivity(),
						// ChatActivity.class).putExtra("userId",
						// contactAdapter.getItem(position).getUsername()));
						startActivity(new Intent(getActivity(),
								ChatActivity.class).putExtra("userId",
								usersNick.get(position).getUid()));
					}
				} else {
					ToastUtil.showShortToast("当前网络不可用，请检查网络连接");
				}
			}
		});

		contactlistView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 隐藏软键盘
				if (getActivity().getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
					if (getActivity().getCurrentFocus() != null)
						inputMethodManager.hideSoftInputFromWindow(
								getActivity().getCurrentFocus()
										.getWindowToken(),
								InputMethodManager.HIDE_NOT_ALWAYS);
				}
				return false;
			}
		});
	}

	/**
	 * 转化拼音
	 * 
	 * @param date
	 * @return
	 */
	private void filledData() {

		for (int i = 0; i < usersNick.size(); i++) {
			if (i >= contactList.size())
				break;
			// 汉字转换成拼音
			String pinyin = characterParser.getSelling(usersNick.get(i)
					.getName());
			String sortString = pinyin.substring(0, 1).toUpperCase();

			// 正则表达式，判断首字母是否是英文字母
			if (sortString.matches("[A-Z]")) {
				usersNick.get(i).setNamePinyin(sortString.toUpperCase());
				contactList.get(i).setHeader(sortString.toUpperCase());
			} else {
				usersNick.get(i).setNamePinyin("#");
				contactList.get(i).setHeader("#");
			}
		}
		Collections.sort(usersNick, pinyinComparator);
		Collections.sort(contactList, pinyinComparatorTwo);

		// 列表中头两条是群和通知 所以从联系人进来的 usersNick真实数据应该从第三条开始（开始两条为空）
		HuanxinUser nullhx = new HuanxinUser();
		nullhx.setNamePinyin("aaa");
		nullhx.setName("aaa");
		usersNick.add(0, nullhx);
		usersNick.add(0, nullhx);

		Map<String, User> users = ApplicationControl.getInstance()
				.getContactList();
		// 加入"申请与通知"和"群聊"
		contactList.add(0, users.get(Constant.GROUP_USERNAME));
		// 把"申请与通知"添加到首位
		contactList.add(0, users.get(Constant.NEW_FRIENDS_USERNAME));
		setlist();
	}

	// **********************通讯录、联系人end********************************************

	@Override
	public void onResume() {
		super.onResume();
		if (!hidden && !((MainTabActivity) getActivity()).isConflict) {
			EMGroupManager.getInstance().loadAllGroups();
			EMChatManager.getInstance().loadAllConversations();
			refresh();
			update_unread_notication();
			dealdd();
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (((MainTabActivity) getActivity()).isConflict) {
			outState.putBoolean("isConflict", true);
		} else if (((MainTabActivity) getActivity())
				.getCurrentAccountRemoved()) {
			outState.putBoolean(Constant.ACCOUNT_REMOVED, true);
		}

	}
}