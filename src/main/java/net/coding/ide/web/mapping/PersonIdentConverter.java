package net.coding.ide.web.mapping;

import net.coding.ide.model.PersonIdent;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

/**
 * Created by tan on 17/05/2017.
 */
@Component
public class PersonIdentConverter extends AbstractConverter<org.eclipse.jgit.lib.PersonIdent, PersonIdent> {

    @Override
    protected PersonIdent convert(org.eclipse.jgit.lib.PersonIdent source) {

        return PersonIdent.builder()
                .name(source.getName())
                .emailAddress(source.getEmailAddress())
                .build();
    }
}
