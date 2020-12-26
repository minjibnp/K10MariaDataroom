package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

public class DataroomDAO {

   Connection con;
   PreparedStatement psmt;
   ResultSet rs;
   
   //기본생성자에서 DBCP(커넥션풀)을 통해 DB연결
   public DataroomDAO() {
      try {
         Context initCtx = new InitialContext();
         //첫번째 룩업은 톰캣 디렉토리를 찾는다
         Context ctx = (Context) initCtx.lookup("java:comp/env");
         //마리아디비 찾는 두번째룩업
         DataSource source = (DataSource) ctx.lookup("jdbc_mariadb");
         con = source.getConnection();
         System.out.println("DBCP연결성공");
      } catch (Exception e) {
         System.out.println("DBCP연결실패");
         e.printStackTrace();
      }
   }
   
	public void close() {
		try {
			//연결을 해제하는것이 아니고 풀에 다시 반납한다.
			if(rs!=null) rs.close();
			if(psmt!=null) psmt.close();
			if(con!=null) con.close();
			
		}
		catch (Exception e) {
			System.out.println("자원반납시 예외발생");
		}
	}
   
   //게시물의 갯수를 카운트
   public int getTotalRecordCount(Map map) {

      int totalCount = 0;
      try {
         String sql = "SELECT COUNT(*) FROM dataroom";
         if (map.get("Word") != null) {
            sql += " WHERE " + map.get("Column") + " " + " LIKE '%" + map.get("Word") + "%'";
         }

         psmt = con.prepareStatement(sql);
         rs = psmt.executeQuery();
         rs.next();
         totalCount = rs.getInt(1);
      } catch (Exception e) {
      }
      return totalCount;
   }
   
   //게시물을 가져와서 ResultSet형태로 반환
   public List<DataroomDTO> selectList(Map map) {

      List<DataroomDTO> bbs = new Vector<DataroomDTO>();
      String sql = "SELECT * FROM dataroom ";
      if (map.get("Word") != null) {
         sql += " WHERE " + map.get("Column") + " " + " LIKE '%" + map.get("Word") + "%'";
      }
      sql += " ORDER BY idx DESC ";
      try {
         // 쿼리 실행후 결과값 반환
         psmt = con.prepareStatement(sql);
         rs = psmt.executeQuery();
         while (rs.next()) {
            DataroomDTO dto = new DataroomDTO();

            dto.setIdx(rs.getString(1));
            dto.setName(rs.getString(2));
            dto.setTitle(rs.getString(3));
            dto.setContent(rs.getString(4));
            dto.setPostdate(rs.getDate(5));
            dto.setAttachedfile(rs.getString(6));
            dto.setDowncount(rs.getInt(7));
            dto.setPass(rs.getString(8));
            dto.setVisitcount(rs.getInt(9));

            bbs.add(dto);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return bbs;
   }
   
   public int insert(DataroomDTO dto)
   {
	   int affected = 0;
	   try {
		   /*
		    MariaDB에서는 시퀀스 대신 auto_increment를 사용하므로
		    해당 쿼리는 삭제한다.
		    */
		   String sql = "INSERT INTO dataroom ("
		   		+ " title, name, content, attachedfile, pass, downcount) "
		   		+ " VALUES ("
		   		+ " ?, ?, ?, ?, ?, 0)";
		   
		   psmt = con.prepareStatement(sql);
		   psmt.setString(1, dto.getTitle());
		   psmt.setString(2, dto.getName());
		   psmt.setString(3, dto.getContent());
		   psmt.setString(4, dto.getAttachedfile());
		   psmt.setString(5, dto.getPass());
		   
		   affected = psmt.executeUpdate();
	   }
	   catch (Exception e) {
		   e.printStackTrace();
	   }
	   return affected;
   }
   //조회수 증가
   public void updateVisitCount(String idx) {
	   
	   String sql = "UPDATE dataroom SET"
	   		+ " visitcount=visitcount+1"
	   		+ " WHERE idx=? ";
	   try {
		   psmt = con.prepareStatement(sql);
		   psmt.setString(1, idx);
		   psmt.executeUpdate();
	   }
	   catch (Exception e) {}
   }
   //자료실 게시물 상세보기
   public DataroomDTO selectView(String idx) {
	   DataroomDTO dto = null;
	   String sql = "SELECT * FROM dataroom "
	   		+ " WHERE idx=?";
	   
	   try {
		   psmt = con.prepareStatement(sql);
		   psmt.setString(1, idx);
		   rs = psmt.executeQuery();
		   if(rs.next()) {
			   dto = new DataroomDTO();
			   
			   dto.setIdx(rs.getString(1));
			   dto.setName(rs.getString(2));
			   dto.setTitle(rs.getString(3));
			   dto.setContent(rs.getString(4));
			   dto.setPostdate(rs.getDate(5));
			   dto.setAttachedfile(rs.getString(6));
			   dto.setDowncount(rs.getInt(7));
			   dto.setPass(rs.getString(8));
			   dto.setVisitcount(rs.getInt(9));//조회수추가
		   }
	   }
	   catch (Exception e) {
		   e.printStackTrace();
	   }
	   return dto;
   }
   
   public DataroomDAO(ServletContext ctx) {
	   try {
			Class.forName(ctx.getInitParameter("MariaJDBCDriver"));
			String id = ctx.getInitParameter("MariaUser");
			String pw = ctx.getInitParameter("MariaPass");
			
			con = DriverManager.getConnection(
					ctx.getInitParameter("MariaConnectURL"),id,pw);
			System.out.println("DB 연결성공!^▽^");
		}
		catch (Exception e) {
			System.out.println("DB 연결실패ㅇ.ㅠ");
			e.printStackTrace();
		}
   }
   
   //게시물의 일련번호, 패스워드를 통한 검증(수정,삭제시 사용됨)
   public boolean isCorrectPassWord(String pass, String idx) {
	   boolean isCorr = true;
	   try {
		   String sql = "SELECT COUNT (*) FROM dataroom "
		   				+ " WHERE pass=? AND idx=?";
		   
		   psmt = con.prepareStatement(sql);
		   psmt.setString(1, pass);
		   psmt.setString(2, idx);
		   rs = psmt.executeQuery();
		   rs.next();
		   if(rs.getInt(1)==0) {
			   //패스워드 검증실패(해당하는 게시물이 없음)
			   isCorr = false;
			   
		   }		   
	   }
	   catch (Exception e) {
		   isCorr = false;
		   e.printStackTrace();
	   }
	   return isCorr;
   }
   
   public int delete(String idx) {
	   int affected = 0;
	   try {
		   String query = "DELETE FROM dataroom "
		   		+ " WHERE idx=?";
		   psmt = con.prepareStatement(query);
		   psmt.setString(1, idx);
		   
		   affected = psmt.executeUpdate();
	   }
	   
	   catch (Exception e) {
		   System.out.println("delete중 예외 발생");
		   e.printStackTrace();
	   }
	   
	   return affected;
   }
   
   public int update(DataroomDTO dto) {
	   int affected = 0;
	   try {
		   String query = "UPDATE dataroom SET"
		   		+ " title=?, name=?, content=? "
		   		+ " , attachedfile=?, pass=? "
		   		+ " WHERE idx=?";
		   
		   psmt = con.prepareStatement(query);
		   psmt.setString(1, dto.getTitle());
		   psmt.setString(2, dto.getName());
		   psmt.setString(3, dto.getContent());
		   psmt.setString(4, dto.getAttachedfile());
		   psmt.setString(5, dto.getPass());
		   
		   //게시물 수정을 위한 추가부분
		   psmt.setString(6, dto.getIdx());
		   
		   affected = psmt.executeUpdate();
	   }
	   catch (Exception e) {
		   System.out.println("update중 예외발생");
		   e.printStackTrace();
	   }
	   
	   return affected;
   }
   //파일 다운로드 횟수증가
   public void downCountPlus(String idx) {
	   String sql = "UPDATE dataroom SET "
	   		+ " downcount=downcount+1 "
	   		+ " WHERE idx=?";
	   try {
		   psmt = con.prepareStatement(sql);
		   psmt.setString(1, idx);
		   psmt.executeUpdate();
	   }
	   catch (Exception e) {}
   }
   
	public List<DataroomDTO> selectListPage(Map map ){
		List<DataroomDTO> bbs = new Vector<DataroomDTO>();
		//쿼리문이 아래와 같이 페이지처리 쿼리문으로 변경됨
		String sql = " "	
				+"SELECT * FROM dataroom ";
			if(map.get("Word")!=null)
			{
				sql += " WHERE "+ map.get("Column") +" "
					+" LIKE '%"+ map.get("Word") + "%' ";
			}
			sql += "	ORDER BY idx DESC LIMIT ?, ?";
				
			System.out.println("쿼리문:"+sql);
			
			try {
				psmt = con.prepareStatement(sql);
				// JSP에서 계산한 페이지 범위값을 이용해 인파라미터를 설정함.
		         psmt.setInt(1,Integer.parseInt(map.get("start").toString()));
		         psmt.setInt(2,Integer.parseInt(map.get("end").toString()));
				
				rs = psmt.executeQuery();
				
				while(rs.next()) {
					DataroomDTO dto = new DataroomDTO();
					
					dto.setIdx(rs.getString(1));
					dto.setName(rs.getString(2));
					dto.setTitle(rs.getString(3));
					dto.setContent(rs.getString(4));
					dto.setPostdate(rs.getDate(5));
					dto.setAttachedfile(rs.getString(6));
					dto.setDowncount(rs.getInt(7));
					dto.setPass(rs.getString(8));
					dto.setVisitcount(rs.getInt(9));
					
					bbs.add(dto);
				}
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			return bbs;
	}
   
}