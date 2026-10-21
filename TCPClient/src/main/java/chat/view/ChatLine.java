package chat.view;

/** One displayable line in the chat list: pure view data, independent of the network layer. */
public record ChatLine(Kind kind, String text) {
    public enum Kind { OWN, OTHER, SYSTEM, ERROR }
}
