package ibm.us.com.chat.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;
import ibm.us.com.chat.R;
import ibm.us.com.chat.common.CommonUtils;
import ibm.us.com.chat.dto.MessageDetailOfMessageThread;

public class InboxSingleThreadRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<MessageDetailOfMessageThread> messageList;
    private Context mContext;
    private Typeface typefaceRobotoBold;
    private Typeface typefaceRobotoRegular;
    private CommonUtils commonUtils;

    private final int VIEW_MESSAGE = 1;
    private final int VIEW_REPLY = 0;

    public InboxSingleThreadRecyclerAdapter(Context context, List<MessageDetailOfMessageThread> messageList) {
        this.messageList = messageList;
        this.mContext = context;
        this.commonUtils = CommonUtils.getInstance();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == VIEW_MESSAGE) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_inbox_thread_message_view, null);
            MessageThreadListRowHolder mh = new MessageThreadListRowHolder(v);
            if(mh.senderName != null && typefaceRobotoBold != null) mh.senderName.setTypeface(typefaceRobotoBold);
            if(mh.date != null && typefaceRobotoRegular != null) mh.date.setTypeface(typefaceRobotoRegular);
            if(mh.description != null && typefaceRobotoRegular != null) mh.description.setTypeface(typefaceRobotoRegular);
            return mh;
        } else {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_inbox_thread_reply_view, null);
            ReplyThreadListRowHolder mh = new ReplyThreadListRowHolder(v);
            if(mh.date != null && typefaceRobotoRegular != null) mh.date.setTypeface(typefaceRobotoRegular);
            if(mh.description != null && typefaceRobotoRegular != null) mh.description.setTypeface(typefaceRobotoRegular);
            return mh;
        }
    }

    public void updateData(List<MessageDetailOfMessageThread> messageListArray, int flag) {
        if (flag == 0) { //append
            for (int i = 0; i < messageListArray.size(); i++) {
                messageList.add(messageListArray.get(i));
            }
            notifyDataSetChanged();
        } else { //clear all
            messageList.clear();
            notifyDataSetChanged();
        }
    }

    public void updateReplyData(MessageDetailOfMessageThread messageObj) {
        if(messageObj != null) messageList.add(0, messageObj);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final MessageDetailOfMessageThread child = messageList.get(position);
        if (holder instanceof MessageThreadListRowHolder) {
            if (child != null) {
                if (!TextUtils.isEmpty(child.getSent())) {
                    String msg = getMSgSentTime(child.getSent());
                    ((MessageThreadListRowHolder) holder).date.setText(Html.fromHtml(msg));
                }

                String name = "";
                if (!TextUtils.isEmpty(child.getFirst_name()) && !TextUtils.isEmpty(child.getLast_name())) {
                    name = child.getFirst_name() + " " + child.getLast_name();
                } else if (!TextUtils.isEmpty(child.getFirst_name())) {
                    name = child.getFirst_name();
                } else if (!TextUtils.isEmpty(child.getLast_name())) {
                    name = child.getLast_name();
                }
                if(!name.equals("")) ((MessageThreadListRowHolder) holder).senderName.setText(name);
                else ((MessageThreadListRowHolder) holder).senderName.setVisibility(View.GONE);

                if (!TextUtils.isEmpty(child.getMessage()))
                    ((MessageThreadListRowHolder) holder).description.setText(Html.fromHtml(addNewLines(child.getMessage())));
            }

        } else {
            if (child != null) {
                if (!TextUtils.isEmpty(child.getReply())) ((ReplyThreadListRowHolder) holder).description.setText(Html.fromHtml(addNewLines(child.getReply())));
                if (!TextUtils.isEmpty(child.getSent())) {
                    String msg = getMSgSentTime(child.getSent());
                    if(msg != null && !msg.equals("")) ((ReplyThreadListRowHolder) holder).date.setText(Html.fromHtml(msg));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return (null != messageList ? messageList.size() : 0);
    }

    public MessageDetailOfMessageThread getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if(messageList.get(position) != null) {
            return (messageList.get(position).getMessage_type().equals("reply")) ? VIEW_REPLY : VIEW_MESSAGE;
        }
        return VIEW_MESSAGE;
    }

    public class MessageThreadListRowHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.txt_sender_name)
        TextView senderName;
        @Bind(R.id.txt_date)
        TextView date;
        @Bind(R.id.txt_msg_description)
        EmojiconTextView description;

        public MessageThreadListRowHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public class ReplyThreadListRowHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.txt_date)
        TextView date;
        @Bind(R.id.txt_msg_description)
        EmojiconTextView description;

        public ReplyThreadListRowHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private String getMSgSentTime(String time) {
        return (commonUtils != null) ? commonUtils.getTimeAgo(time) : "<p></p>";
    }

    private String addNewLines(String message) {
        return message.replace("\n", "<br/>");
    }
}
