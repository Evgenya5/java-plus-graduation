package comment.service;

import comment.dto.CommentDto;
import comment.dto.CommentStatusUpdateDto;
import comment.dto.NewCommentDto;
import comment.dto.UpdateCommentDto;
import comment.model.CommentStatus;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteCommentByOwner(Long userId, Long commentId);

    List<CommentDto> getOwnerComments(Long userId);

    CommentDto moderateComment(Long commentId, CommentStatusUpdateDto updateDto);

    void deleteCommentByAdmin(Long commentId);

    List<CommentDto> getCommentsForAdmin(CommentStatus status, Long eventId, Long userId);

    List<CommentDto> getPublishedEventComments(Long eventId);

    CommentDto getPublishedComment(Long commentId);
}
