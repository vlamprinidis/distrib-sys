package beans;

/*
 * Message tags
 */
public enum MessageType {
    IdRequest,
    IdResponse,
    PeerJoin,
    JoinAnswer,
    BalanceRequest,
    BalanceResponse,
    PeerInfoRequest,
    PeerInfoResponse,
    CliTsxRequest,
    CliTsxResponse,
    NewTransaction,
    Ping,
    Pong,
    BlockMined,
    Stop
}
