package com.finalist.cmsc.paging;

import org.apache.commons.lang.StringUtils;
import com.finalist.cmsc.mmbase.PropertiesUtil;

public class PagingStatusHolder {
   private int page;
   private int pageCount;
   private int pageSize = -1;
   private int listSize;
   private String sort;
   private String dir = "asc";

   public int getPage() {
      return page;
   }

   public void setPage(int page) {
      this.page = page;
   }

   public int getPageCount() {

      if (pageCount > 0) {
         return pageCount;
      }

      if (listSize < getPageSize()) {
         return 1;
      }

      int remainder = listSize % getPageSize();

      if (0 == remainder) {
         return listSize / getPageSize();
      }
      else {
         return listSize / getPageSize() + 1;
      }
   }

   public void setPageCount(int pageCount) {
      this.pageCount = pageCount;
   }

   public int getPageSize() {
      String defalutPagesize = PropertiesUtil.getProperty("repository.search.results.per.page");
      if (pageSize < 0 && StringUtils.isBlank(defalutPagesize)) {
         return 50;
      }
      else if (pageSize < 0) {
         return Integer.parseInt(defalutPagesize);
      }
      else {
         return this.pageSize;
      }
   }

   public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
   }

   public String getSort() {
      return sort;
   }

   public void setSort(String sort) {
      this.sort = sort;
   }

   public int getListSize() {
      return listSize;
   }

   public void setListSize(int listSize) {
      this.listSize = listSize;
   }

   public int getOffset(){
      return page*getPageSize();      
   }

   public String getDir() {
      return dir;
   }

   public void setDir(String dir) {
      this.dir = dir;
   }

   public void setDefaultSort(String column, String direction) {
      if(StringUtils.isBlank(this.sort)&& StringUtils.isNotBlank(column)){
         this.setSort(column);

         String dir = StringUtils.isNotBlank(direction)?direction : "asc";
         this.setDir(dir);
      }
   }
}
