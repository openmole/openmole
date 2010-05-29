package org.simexplorer.ide.ui.dataexplorer.factors;

import javax.swing.JPanel;
import org.openmole.core.model.domain.IDomain;

public class DomainPanel extends JPanel{
    IDomain domain;
    Class domainType;

    public IDomain getDomain() {
        return domain;
    }

    public void setDomain(IDomain domain) {
        this.domain = domain;
    }

    public Class getDomainType() {
        return domainType;
    }

    public void setDomainType(Class domainType) {
        this.domainType = domainType;
    }

}
