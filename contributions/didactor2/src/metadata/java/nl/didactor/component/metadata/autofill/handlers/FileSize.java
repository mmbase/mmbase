package nl.didactor.component.metadata.autofill.handlers;


import org.mmbase.bridge.*;

import nl.didactor.component.metadata.autofill.Handler;


public class FileSize extends Handler{


    protected void handlerAddMetaData(Node nodeLangString, Node nodeObject){
        int iFileSize = nodeObject.getByteValue("handle").length;

        nodeLangString.setStringValue("value", convertToHumanFormat(iFileSize));
        nodeLangString.commit();
    }


    protected boolean handlerCheckMetaData(Node nodeLangString, Node nodeObject){
        int iFileSize = nodeObject.getByteValue("handle").length;

        if(convertToHumanFormat(iFileSize).equals(nodeLangString.getValue("value"))){
            return true;
        }
        else {
            return false;
        }
    }


    /**
     * Converts size in bytes to readable format
     * @param iFileSize int
     * @return String
     */
    private String convertToHumanFormat(int iFileSize){
        if(iFileSize < 1024){
            return "" + iFileSize;
        }
        if(iFileSize < 1048576){
            return "" + (new Double((iFileSize * 100 + 0.5) / 1024)).intValue() / 100.0 + "K";
        }
        if(iFileSize >= 1048576){
            return "" + (new Double((iFileSize * 100 + 0.5) / 1048576)).intValue() / 100.0 + "M";
        }
        return "-1";
    }
}
