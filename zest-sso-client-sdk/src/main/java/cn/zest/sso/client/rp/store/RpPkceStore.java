package cn.zest.sso.client.rp.store;

/**
 * Authorization Code + PKCE state 存储。
 */
public interface RpPkceStore {

    void save(String state, String codeVerifier);

    /** 消费并删除，不存在或已过期返回 null */
    String consume(String state);
}
