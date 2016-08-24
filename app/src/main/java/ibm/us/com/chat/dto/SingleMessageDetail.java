package ibm.us.com.chat.dto;

import org.parceler.Parcel;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by chamith_d on 7/11/2016.
 */
@Getter
@Setter
@Parcel
public class SingleMessageDetail {
    private String message_type;
    private String sender_id;
    private String first_name;
    private String last_name;
    private String msg_id;
    private String message;
    private String message_read_status;
    private String reply_id;
    private String reply;
    private String sent;
    private String profile_image;
    private String isLaunchFromNotification;
}
