package comment.service;

import comment.client.ResilientEventClient;
import comment.client.ResilientUserClient;
import comment.dto.CommentDto;
import comment.dto.CommentStatusUpdateDto;
import comment.dto.NewCommentDto;
import comment.dto.UpdateCommentDto;
import comment.mapper.CommentMapper;
import comment.model.Comment;
import comment.model.CommentStatus;
import comment.repository.CommentRepository;
import ewm.event.client.dto.InternalEventDto;
import ewm.user.client.dto.UserDto;
import ewm.common.exception.AccessDeniedException;
import ewm.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ResilientUserClient userClient;
    private final ResilientEventClient eventClient;
    private final CommentMapper commentMapper;

    @Override
    public CommentDto createComment(Long userId, NewCommentDto newCommentDto) {
        UserDto author = getUser(userId);
        eventClient.getEventById(newCommentDto.getEventId());
        Comment comment = commentMapper.toEntity(newCommentDto, userId);
        comment = commentRepository.save(comment);
        CommentDto commentDto = commentMapper.toDto(comment);
        commentDto.setAuthorName(author.getName());
        return commentDto;
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = getComment(commentId);
        validateCommentOwner(comment, userId);
        UserDto user = getUser(userId);
        validateCommentCanBeUpdated(comment);
        comment.setText(updateCommentDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment = commentRepository.save(comment);
        CommentDto commentDto = commentMapper.toDto(comment);
        commentDto.setAuthorName(user.getName());
        return commentDto;
    }

    @Override
    public void deleteCommentByOwner(Long userId, Long commentId) {
        Comment comment = getComment(commentId);
        validateCommentOwner(comment, userId);
        markAsDeleted(comment);
    }

    @Override
    public List<CommentDto> getOwnerComments(Long userId) {
        UserDto user = getUser(userId);
        String authorName = user != null ? user.getName() : null;
        List<Comment> comments = commentRepository.findByAuthorId(userId);

        return comments.stream()
                .map(c -> {
                    CommentDto dto = commentMapper.toDto(c);
                    dto.setAuthorName(authorName);
                    return dto;
                })
                .toList();
    }

    @Override
    public CommentDto moderateComment(Long commentId, CommentStatusUpdateDto updateDto) {
        Comment comment = getComment(commentId);
        validateCommentCanBeModerated(comment);
        validateModerationStatus(updateDto.getStatus());
        comment.setStatus(updateDto.getStatus());
        comment.setUpdated(LocalDateTime.now());
        comment = commentRepository.save(comment);
        String authorName = getUser(comment.getAuthorId()).getName();
        CommentDto commentDto = commentMapper.toDto(comment);
        commentDto.setAuthorName(authorName);
        return commentDto;
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = getComment(commentId);
        markAsDeleted(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForAdmin(CommentStatus status, Long eventId, Long userId) {
        return commentRepository.findCommentsByFilters(status, eventId, userId).stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getPublishedEventComments(Long eventId) {
        getEvent(eventId);
        return commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED).stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getPublishedComment(Long commentId) {
        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found or not published"));
        return commentMapper.toDto(comment);
    }

    private UserDto getUser(Long userId) {
        return userClient.getUserById(userId);
    }

    private InternalEventDto getEvent(Long eventId) {
        return eventClient.getEventById(eventId);
    }

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));
    }

    private void validateCommentOwner(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("User with id=" + userId + " is not the owner of comment with id=" + comment.getId());
        }
    }

    private void validateCommentCanBeUpdated(Comment comment) {
        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new IllegalStateException("Can only update PENDING comments. Current status: " + comment.getStatus());
        }
    }

    private void validateCommentCanBeModerated(Comment comment) {
        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new IllegalStateException("Can only moderate PENDING comments. Current status: " + comment.getStatus());
        }
    }

    private void validateModerationStatus(CommentStatus status) {
        if (status != CommentStatus.PUBLISHED && status != CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid status for moderation. Only PUBLISHED or REJECTED allowed. Received: " + status);
        }
    }

    private void markAsDeleted(Comment comment) {
        if (comment.getStatus() != CommentStatus.DELETED) {
            comment.setStatus(CommentStatus.DELETED);
            comment.setUpdated(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }
}
