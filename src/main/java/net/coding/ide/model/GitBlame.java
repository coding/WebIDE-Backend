package net.coding.ide.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by tan on 03/05/2017.
 */
@Builder
@Data
public class GitBlame {

    private PersonIdent author;

    private String shortName;

}
