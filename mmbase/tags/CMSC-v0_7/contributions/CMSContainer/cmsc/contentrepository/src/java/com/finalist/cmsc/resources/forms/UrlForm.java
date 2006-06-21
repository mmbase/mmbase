package com.finalist.cmsc.resources.forms;


@SuppressWarnings("serial")
public class UrlForm extends SearchForm {
    
    private String name;
    private String description;
    private String url;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    
    public String getName() {
        return name;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    
    public String getUrl() {
        return url;
    }

    
    public void setUrl(String url) {
        this.url = url;
    }
}
