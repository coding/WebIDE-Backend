package net.coding.ide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by tan on 16/05/2017.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonIdent {
    private String name;

    private String emailAddress;
}