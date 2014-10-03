package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderPostList extends ArrayList<ReaderPost> {

    private int indexOfPost(long blogId, long postId) {
        for (int i = 0; i < size(); i++) {
            if (this.get(i).blogId == blogId && this.get(i).postId == postId) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfPost(ReaderPost post) {
        if (post == null) {
            return -1;
        }
        return indexOfPost(post.blogId, post.postId);
    }

    /*
     * does passed list contain the same posts as this list?
     */
    public boolean isSameList(ReaderPostList posts) {
        if (posts == null || posts.size() != this.size()) {
            return false;
        }

        for (ReaderPost post: posts) {
            int index = indexOfPost(post.blogId, post.postId);
            if (index == -1) {
                return false;
            }
            ReaderPost thisPost = this.get(index);
            if (thisPost.numLikes != post.numLikes
                    || thisPost.numReplies != post.numReplies
                    || thisPost.isFollowedByCurrentUser != post.isFollowedByCurrentUser
                    || thisPost.isLikedByCurrentUser != post.isLikedByCurrentUser
                    || thisPost.isRebloggedByCurrentUser != post.isRebloggedByCurrentUser) {
                return  false;
            }
        }

        return true;
    }

}
