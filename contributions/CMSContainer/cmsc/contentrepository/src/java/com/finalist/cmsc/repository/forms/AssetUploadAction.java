package com.finalist.cmsc.repository.forms;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.mmbase.bridge.Cloud;
import org.mmbase.bridge.NodeManager;

import com.finalist.util.http.BulkUploadUtil;

public class AssetUploadAction extends AbstractUploadAction {

   @Override
   public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
         HttpServletResponse response, Cloud cloud) throws Exception {

      AssetUploadForm assetUploadForm = (AssetUploadForm) form;
      String parentchannel = assetUploadForm.getParentchannel();
      FormFile file = assetUploadForm.getFile();

      String exceed = "no";

      if (file.getFileSize() != 0 && file.getFileName() != null) {
         String assetType = "";
         if (isImage(file.getFileName())) {
            assetType = "images";
         } else {
            assetType = "attachments";
         }

         List<Integer> nodes = null;
         NodeManager manager = cloud.getNodeManager(assetType);

         int fileSize = file.getFileSize();
         if (maxFileSizeBiggerThan(fileSize)) {
            if (isNewFile(file, manager)) {
               nodes = BulkUploadUtil.store(cloud, manager, parentchannel, file);
               request.setAttribute("uploadedAssets", nodes);
            } else {
               return new ActionForward(mapping.findForward(SUCCESS).getPath()
                     + "?type=asset&direction=down&exist=1&exceed=" + exceed + "&parentchannel=" + parentchannel, true);
            }
         } else {
            exceed = "yes";
         }
         // to archive the upload asset
         addRelationsForNodes(nodes, cloud);
      }
      return new ActionForward(mapping.findForward(SUCCESS).getPath() + "?type=asset&direction=down&exist=0&exceed="
            + exceed + "&parentchannel=" + parentchannel, true);
   }
}
