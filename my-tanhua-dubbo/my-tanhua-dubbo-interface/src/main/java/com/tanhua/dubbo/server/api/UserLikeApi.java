package com.tanhua.dubbo.server.api;

public interface UserLikeApi {
    /**
     * 保存喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    String saveUserLike(Long userId, Long likeUserId);


    /**
     * 相互喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    Boolean isMutualLike(Long userId, Long likeUserId);

    /**
     * 删除用户喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    Boolean deleteUserLike(Long userId, Long likeUserId);
}
