package net.coding.ide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by tan on 19/05/2017.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitRef {

    private String name;

    private String id;

}
