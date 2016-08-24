package sacdulanga.us.com.chat.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.badoo.mobile.util.WeakHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import sacdulanga.us.com.chat.R;
import sacdulanga.us.com.chat.common.CommonUtils;
import sacdulanga.us.com.chat.dto.ChatMessage;
import sacdulanga.us.com.chat.dto.MessageDetailOfMessageThread;
import sacdulanga.us.com.chat.dto.SingleMessageDetail;

public class CustomChatActivity extends AppCompatActivity implements BaseBackPressedListener.OnBackPressedListener {

    final String TAG = CustomChatActivity.this.getClass().getSimpleName();
    private static final String         BLUEMIX = "ws://***.***.***.**:5599/chat";
    private static final String         CLIENT_PREFIX = "chat_";
    private static final String         KEY_DATA = "data";
    private static final String         KEY_PAYLOAD = "payload";

    private final WebSocketConnection   socket = new WebSocketConnection();

    private int                         blue;
    private int                         green;
    private int                         red;
    private String                      client;
    private EditText                    txtContent;
    private WeakHandler                 handler;

    private SingleMessageDetail         mSingleMessageDetail;
    private InboxSingleThreadRecyclerAdapter mInboxSingleThreadRecyclerAdapter;
    private Typeface                    rsRTypeface;
    private MessageDetailOfMessageThread mReplyThreadDetail = null;
    private LinearLayoutManager         mLayoutManager;
    protected RelativeLayout            loadingView;
    protected Toolbar                   mToolBar;
    private boolean                     recycleScrolled = false;
    private boolean                     isKeyBoardOpened = false;
    private int                         page = 0;
    private EmojIconActions             emojIcon;

    TextView toolBarTitle;

    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.msg_edit_text_container)
    RelativeLayout messageContainer;
    @Bind(R.id.parent_layout)
    RelativeLayout parentLayout;
    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @Bind(R.id.txtMsg)
    EmojiconEditText message;
    @Bind(R.id.message_box_empty)
    TextView mMessageBoxEmptyMessage;
    @Bind(R.id.btn_send_loading)
    ProgressBar btnSendProgress;
    @Bind(R.id.btn_save)
    Button btnSave;
    @Bind(R.id.emoji_btn)
    ImageView emojiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_inbox_single_thread);
        ButterKnife.bind(this);
        getActionBarToolbar();
        setUpUI();
        setUpToolBar();
        // Client ID
        client = CLIENT_PREFIX + System.currentTimeMillis();

        // Identifying color
        red = (int)Math.round(Math.random() * 255);
        green = (int)Math.round(Math.random() * 255);
        blue = (int)Math.round(Math.random() * 255);

        // Handler
        handler = new WeakHandler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                Bundle      bundle;
                ChatMessage chat;
                JSONArray   history;
                JSONObject  data;
                JSONObject  item;

                bundle = message.getData();

                try {
                    // JSON object
                    data = new JSONObject(bundle.getString(KEY_PAYLOAD));

                    // Evaluate action
                    switch(data.getString(ChatMessage.KEY_ACTION)) {
                        // Whole history
                        case ChatMessage.ACTION_HISTORY:
                            history = data.getJSONArray(KEY_DATA);

                            for(int h = 0; h < history.length(); h++) {
                                item = history.getJSONObject(h);

                                chat = new ChatMessage();
                                chat.client = item.getString(ChatMessage.KEY_CLIENT);
                                chat.red = item.getInt(ChatMessage.KEY_RED);
                                chat.green = item.getInt(ChatMessage.KEY_GREEN);
                                chat.blue = item.getInt(ChatMessage.KEY_BLUE);
                                chat.message = item.getString(ChatMessage.KEY_MESSAGE);

//                                items.add(chat);
                            }

                            break;

                        // Single item
                        case ChatMessage.ACTION_CREATE:
                            item = data.getJSONObject(KEY_DATA);

                            chat = new ChatMessage();
                            chat.client = item.getString(ChatMessage.KEY_CLIENT);
                            chat.red = item.getInt(ChatMessage.KEY_RED);
                            chat.green = item.getInt(ChatMessage.KEY_GREEN);
                            chat.blue = item.getInt(ChatMessage.KEY_BLUE);
                            chat.message = item.getString(ChatMessage.KEY_MESSAGE);

//                            items.add(chat);

                            break;
                    }
                } catch(JSONException jsone) {
                    jsone.printStackTrace();
                }
                return false;
            }
        });

        emojIcon=new EmojIconActions(this,parentLayout,message,emojiButton);
        emojIcon.ShowEmojIcon();
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
                Log.e("Keyboard","open");
            }

            @Override
            public void onKeyboardClose() {
                Log.e("Keyboard","close");
            }
        });


        // Text field
        txtContent = (EditText) findViewById(R.id.txtMsg);
        txtContent.setTextColor(Color.rgb(
            red,
            green,
            blue
        ));
        txtContent.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                ChatMessage chat = null;
                // Send
                if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Message present
                    sendMessage(chat);
                }
                return false;
            }
        });

        // WebSockets
        try {
            socket.connect(BLUEMIX, new WebSocketHandler() {
                @Override
                public void onOpen() {
                    ChatMessage chat;

                    // Debug
                    Log.d("WEBSOCKETS", "Connected to server.");

                    // Message for chat history
                    chat = new ChatMessage();
                    chat.action = ChatMessage.ACTION_HISTORY;

                    // Send request
                    socket.sendTextMessage(chat.toJSON());
                }

                @Override
                public void onTextMessage(String payload) {
                    Bundle      bundle;
                    Message     message;

                    Log.d("WEBSOCKETSSS", payload);

                    mReplyThreadDetail = createNewMessageList(payload,false);
                    if (mInboxSingleThreadRecyclerAdapter != null) {
                        recyclerViewScrollBottom();
                        mInboxSingleThreadRecyclerAdapter.updateReplyData(mReplyThreadDetail);
                    }
                    recycleScrolled = false;

                    bundle = new Bundle();
                    bundle.putString(KEY_PAYLOAD,payload);

                    message = new Message();
                    message.setData(bundle);

                    handler.sendMessage(message);
                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d("WEBSOCKETS", "Connection lost.");
                }
            });
        } catch(WebSocketException wse) {
            Log.d("WEBSOCKETS", wse.getMessage());
        }
    }

    public void sendMessage(ChatMessage chat){
        if(txtContent.getText().toString().trim().length() > 0) {
            // Build message
            chat = new ChatMessage();
            chat.action = ChatMessage.ACTION_CREATE;
            chat.client = client;
            chat.red = red;
            chat.green = green;
            chat.blue = blue;
            chat.message = txtContent.getText().toString();
            chat.css = "rgb( " + red + ", " + green + ", " + blue + " )";

            // Publish
            mReplyThreadDetail = createNewMessageList(chat.message,true);
            socket.sendTextMessage(chat.toJSON());
            if (mInboxSingleThreadRecyclerAdapter != null) {
                recyclerViewScrollBottom();
                mInboxSingleThreadRecyclerAdapter.updateReplyData(mReplyThreadDetail);
            }
            recycleScrolled = false;
            // Clear field
            txtContent.setText(null);
        }
    }

    private MessageDetailOfMessageThread createNewMessageList(String message ,boolean type) {
        MessageDetailOfMessageThread child = new MessageDetailOfMessageThread();
        try {
           if(type) {
               child.setMessage_type("reply");
               child.setReply(message);
           }else{
               child.setMessage_type("received");
               child.setMessage(message);
           }
            child.setSent(CommonUtils.getInstance().getDateTimeFormatter().format(getCurrentUTCTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return child;
    }

    @OnClick(R.id.btn_save)
    public void requestSendMessage(View view) {
        if(!message.getText().toString().trim().matches("")) {
            ChatMessage chat = null;
            sendMessage(chat);
        }
        dismissSoftKeyBoard();
    }

    public Date getCurrentUTCTime() throws ParseException {
        String fromTimeZone = "UTC";
        SimpleDateFormat dateFormatGmt = CommonUtils.getInstance().getDateTimeFormatter();
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone(fromTimeZone));
        return CommonUtils.getInstance().formatDate(dateFormatGmt.format(new Date()));
    }

    private void setMsgIcon(Button button, int drawable) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN)
            button.setBackgroundDrawable(getResources().getDrawable(drawable));
        else
            button.setBackground(getResources().getDrawable(drawable));
    }

    public void setKeyBoardListenerToRootView() {
        try {
            final View activityRootView = parentLayout;
            activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    try {
                        int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                        if (heightDiff > CommonUtils.getInstance().dpToPx(200)) { // if more than 200 dp, it's probably a keyboard...
                            isKeyBoardOpened = true;
                        } else if (isKeyBoardOpened) {
                            if (message != null) message.clearFocus();
                            isKeyBoardOpened = false;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (Exception ex) {
            isKeyBoardOpened = false;
            Log.e(TAG, "setKeyBoardListenerToRootView: " + ex.toString());
        }
    }

    private void recyclerViewScrollBottom() {
        if(mRecyclerView == null) return;
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!recycleScrolled && mInboxSingleThreadRecyclerAdapter != null && mInboxSingleThreadRecyclerAdapter.getItemCount() > 0) {
                        mRecyclerView.scrollToPosition(0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "recyclerViewScrollBottom: " + e.toString());
                }
            }
        }, 100);
    }

    private void recyclerViewScrollBottom_() {
        if(mRecyclerView == null) return;
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!recycleScrolled && mInboxSingleThreadRecyclerAdapter != null && mInboxSingleThreadRecyclerAdapter.getItemCount() > 0) {
                        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, 0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "recyclerViewScrollBottom_: " + e.toString());
                }
            }
        }, 1000);
    }

    private void dismissSoftKeyBoard() {
        try {
            InputMethodManager im = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getSenderName(String firstName, String lastName) {
        if(firstName != null || lastName != null) {
            if(!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) return firstName + " " + lastName;
            else if(!TextUtils.isEmpty(firstName)) return firstName;
            else if(!TextUtils.isEmpty(lastName)) return lastName;
        }
        return "";
    }

    protected void setUpUI() {
        try {
            initRecyclerView();
            setKeyBoardListenerToRootView();

            int[] colors = getResources().getIntArray(R.array.google_colors);
            mSwipeRefreshLayout.setColorSchemeColors(colors);

            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (CommonUtils.getInstance().isNetworkConnected()) {
                    } else {
                    }

                }
            });

            setLoading(true);

            message.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        recyclerViewScrollBottom_();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() { // have to set recycleScrolled to false because
                                recycleScrolled = false; // when scroll to bottom manual scroll will trigger and sets recycleScrolled to true
                            }
                        }, 3000);
                    }
                }
            });

            message.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    recyclerViewScrollBottom();
                    return false;
                }
            });

            message.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.toString().trim().length() != 0 && btnSave.getTag().toString().equals("0")) {
                        setMsgIcon(btnSave, R.drawable.bg_inbox_send_msg);
                        btnSave.setTag(1);
                    } else if (s.toString().trim().length() == 0) {
                        setMsgIcon(btnSave, R.drawable.bg_inbox_send_disable_msg);
                        btnSave.setTag(0);
                    }
                }
            });

        } catch (Exception ex) {
            Log.e(TAG, "setUpUI: " + ex.toString());
        }
    }

    protected void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        mInboxSingleThreadRecyclerAdapter = new InboxSingleThreadRecyclerAdapter(this, new ArrayList<MessageDetailOfMessageThread>());
        mRecyclerView.setAdapter(mInboxSingleThreadRecyclerAdapter);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (mLayoutManager.findFirstVisibleItemPosition() >= 2 && !recycleScrolled) {
                    recycleScrolled = true;
                }
            }
        });
    }

    protected void setUpToolBar() {
        View mCustomView = this.getLayoutInflater().inflate(R.layout.custom_actionbar_inbox_single_thread, null);
        ((TextView) mCustomView.findViewById(R.id.title)).setTypeface(rsRTypeface);

        toolBarTitle = (TextView) mCustomView.findViewById(R.id.title);
//        toolBarTitle.setText(getSenderName(mSingleMessageDetail.getFirst_name(), mSingleMessageDetail.getLast_name()));
        toolBarTitle.setText("My Test chat");
        Button btnBack = (Button) mCustomView.findViewById(R.id.btn_back);
        btnBack.bringToFront();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSingleMessageDetail != null && mSingleMessageDetail.getIsLaunchFromNotification() != null) {
//                    goToInbox();
                } else {
                   CustomChatActivity.this.getSupportFragmentManager().popBackStack();
                }
            }
        });
        mToolBar.removeAllViews();
        mToolBar.addView(mCustomView);
    }

    public void setLoading(boolean isLoading) {
        if (loadingView != null) loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    protected void setupLoading() {
        loadingView = (RelativeLayout)findViewById(R.id.rl_progress);
    }

    @Override
    public void doBack() {

    }

    protected Toolbar getActionBarToolbar() {
        mToolBar = (Toolbar)findViewById(R.id.toolbar);
        if (mToolBar != null) {
            this.setSupportActionBar(mToolBar);
            mToolBar.setContentInsetsAbsolute(0, 0); /** remove actionbar unnecessary left margin */
        }
        return mToolBar;
    }

}
