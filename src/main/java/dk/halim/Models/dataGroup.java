package dk.halim.Models;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class dataGroup {

    private String name;
    private String EFName;
    private String shortEFIdentifier;
    private String EFIdentifier;
    private String tag;

    public dataGroup(String name, String EFName, String shortEFIdentifier, String EFIdentifier, String tag){
        this.name = name;
        this.EFName = EFName;
        this.shortEFIdentifier = shortEFIdentifier;
        this.EFIdentifier = EFIdentifier;
        this.tag = tag;
    }

    public String getEFName(){
        return this.EFName;
    }

}
