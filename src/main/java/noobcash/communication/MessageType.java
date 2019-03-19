package noobcash.communication;

/*
 * Message tags
 */
public enum MessageType {
    IdRequest,
    IdResponse,
    JoinRequest,
    JoinResponse,
    BalanceRequest,
    BalanceResponse,
    PeerInfoRequest,
    PeerInfoResponse,
    CliTsxRequest,
    CliTsxResponse,
    LastBlockRequest,
    LastBlockResponse,
    ChainRequest,
    ChainResponse,
    BlockToMine,
    BlockMined,
    NewTransaction,
    NewBlock,
    Stop
}
