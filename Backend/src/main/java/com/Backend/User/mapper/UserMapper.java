package com.Backend.User.mapper;

import com.Backend.User.dto.UserDTO;
import com.Backend.User.entity.User;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    @Autowired
    private ModelMapper modelMapper;

     /**
     * El metodo (modelMapper) en este caso se encarga de pasar de un objeto Usuario a uno objeto DTO(Linea 28) y viceversa (Linea 36),
     * SetFieldAccessLevel: sirve para acceder a variables privadas,
     * (MatchingStrategies.STANDARD): sirve para que cuando se realize la conversion
     * entre los objetos busque los objetos que tengan el mismo nombre, tambien se puede usar LOOSE
     * es un poco menos restrictivo en los nombres,
     * setSkipNullEnabled: sirve para que cuando llegue un atributo en null no actualize el Atributo en cuestion,
     * Beneficios: codigo mas entendible y ahorro de lineas y tiempo.
     * @param user
     * @return UserDTO
     */
    public UserDTO convertToDto(User user) {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setSkipNullEnabled(true);
        return modelMapper.map(user, UserDTO.class);
    }

    public User convertToEntity(UserDTO userDTO) {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setSkipNullEnabled(true);
        return modelMapper.map(userDTO, User.class);
    }

    public void updateEntity(UserDTO dto, User entity) {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setSkipNullEnabled(true);
        modelMapper.map(dto, entity);
    }
}
