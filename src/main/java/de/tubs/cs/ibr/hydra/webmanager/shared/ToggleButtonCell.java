package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.github.gwtbootstrap.client.ui.ButtonCell;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ToggleButtonCell extends ButtonCell {
    
    private boolean enabled;
    
    public ToggleButtonCell()
    {
        super();
        enabled = true;
    }
    public ToggleButtonCell(boolean enabled)
    {
        super();
        this.enabled = enabled;
    }
    
    public ToggleButtonCell(ButtonType type) {
        super(type);
        enabled = true;
    }

    public ToggleButtonCell(IconType icon) {
        super(icon);
        enabled = true;
    }
    
    public ToggleButtonCell(ButtonSize size) {
        super(size);
        enabled = true;
    }

    public ToggleButtonCell(IconType icon, ButtonType type) {
        super(icon,type);
        enabled = true;
    }
    
    public ToggleButtonCell(IconType icon, ButtonSize size) {
        super(icon,size);
        enabled = true;
    }
    
    public ToggleButtonCell(ButtonType type, ButtonSize size) {
        super(type,size);
        enabled = true;
    }
    
    public ToggleButtonCell(IconType icon, ButtonType type, ButtonSize size) {
        super(icon,type,size);
        enabled = true;
    }

	@Override
	public void render(Context context, SafeHtml data, SafeHtmlBuilder sb) {

	    String html_disabled = "";
	    if (!enabled)
	        html_disabled = "disabled ";
	    
        sb.appendHtmlConstant("<button " + html_disabled + "type=\"button\" class=\"btn "
                + (super.getType() != null ? super.getType().get() : "") + (super.getSize() != null ? " " + super.getSize().get() : "") + "\" tabindex=\"-1\">");
        if (data != null) {
            if (super.getIcon() != null) {
                sb.appendHtmlConstant("<i class=\"" + super.getIcon().get() + "\"></i> ");
            }
            sb.append(data);
        }
        sb.appendHtmlConstant("</button>");
    } 
	
	public void setEnabled(boolean set)
	{
	    enabled = set;
	}
	
	public boolean getEnabled()
	{
	    return enabled;
	}

}
