package net.coding.ide.model;

import lombok.Data;

/**
 * Created by tan on 2016/9/21.
 */
@Data
public class GitLog {

    private String shortName;

    private String name;

    private String shortMessage;

    private int commitTime;

    private PersonIdent commiterIdent;

    private PersonIdent authorIdent;

    private String[] parents;
}
