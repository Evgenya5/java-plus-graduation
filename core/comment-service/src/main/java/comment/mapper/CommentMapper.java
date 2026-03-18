package comment.mapper;

import comment.dto.CommentDto;
import comment.dto.NewCommentDto;
import comment.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "authorName", ignore = true)
    CommentDto toDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "created", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "text", source = "newCommentDto.text")
    @Mapping(target = "eventId", source = "newCommentDto.eventId")
    @Mapping(target = "authorId", source = "authorId")
    Comment toEntity(NewCommentDto newCommentDto, Long authorId);
}
