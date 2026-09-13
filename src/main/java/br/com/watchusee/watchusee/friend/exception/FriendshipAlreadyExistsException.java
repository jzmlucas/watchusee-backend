package br.com.watchusee.watchusee.friend.exception;

public class FriendshipAlreadyExistsException extends RuntimeException {

    public FriendshipAlreadyExistsException(String message) {
        super(message);
    }
}
