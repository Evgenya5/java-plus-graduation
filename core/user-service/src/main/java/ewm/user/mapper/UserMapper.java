package ewm.user.mapper;

import org.mapstruct.Mapper;
import ewm.user.client.dto.UserDto;
import ewm.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toUserDto(User user);

    User toUser(UserDto userShortDto);
}
