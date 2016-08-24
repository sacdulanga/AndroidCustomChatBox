package ibm.us.com.chat.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by chamith_d on 7/11/2016.
 */
@Getter
@Setter
public class MessageDetailOfMessageThread {
    private String message_type;
    private String sender_id;
    private String first_name;
    private String last_name;
    private String msg_id;
    private String message;
    private String reply_id;
    private String reply;
    private String sent;
}
