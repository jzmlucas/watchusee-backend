package br.com.watchusee.watchusee.share.exception;

public class ShareRecipientNotFoundException
        extends RuntimeException {

    public ShareRecipientNotFoundException(String message) {
        super(message);
    }
}