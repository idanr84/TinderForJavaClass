/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fbapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fbapp.model.*;
import com.google.gson.Gson;

/**
 *
 * @author Rameez Usmani
 */
@WebServlet(name = "MeController", urlPatterns = {"/me"})
public class MeController extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String accessToken=request.getHeader("Authorization");
        if (accessToken==null){
            ApiResult result=new ApiResult("400","Authorization header missing",null);
            ServletHelper.writeResponse(result,response);
            return;
        }
        DbHelper db=new DbHelper();
        try{
            db.open();
            User user=db.getUserByToken(accessToken);
            if (user==null){
                db.close();
                ApiResult result=new ApiResult("403","Invalid access token",null);
                ServletHelper.writeResponse(result,response);
            }else{
                MeModel me=new MeModel();
                try{
                    user.images=db.getUserImages(user.id);
                    me.user=user;
                    me.potential_matches=db.getPotentialMatchingUsers(user.id);
                    for (int a=0;a<me.potential_matches.size();a++){
                        PotentialMatchUser pmu=me.potential_matches.get(a);
                        pmu.images=db.getUserImages(pmu.id);
                        me.potential_matches.set(a,pmu);
                    }
                    me.matched_users=db.getMatchedUsers(user.id);
                    for (int a=0;a<me.matched_users.size();a++){
                        UserMatch pmu=me.matched_users.get(a);
                        pmu.user.images=db.getUserImages(pmu.user.id);
                        me.matched_users.set(a,pmu);
                    }
                    db.close();
                    //response.getWriter().write("Helo");
                    ApiResult result=new ApiResult("200",null,me);
                    ServletHelper.writeResponse(result,response);
                }catch(Exception ex){
                    db.close();
                    //response.getWriter().write("Helo: "+ex.getMessage());
                    throw ex;
                }
            }
        }catch(Exception ex){
            ApiResult result=new ApiResult("400",ex.getMessage(),null);
            ServletHelper.writeResponse(result,response);
        }
    }
    
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String accessToken=request.getHeader("Authorization");
        if (accessToken==null){
            ApiResult result=new ApiResult("400","Authorization header missing",null);
            ServletHelper.writeResponse(result,response);
            return;
        }
        DbHelper db=new DbHelper();
        try{
            String jsonBody=ServletHelper.getBodyAsString(request);
            //response.getWriter().write(jsonBody);
            Gson gs=new Gson();
            SubmitModel sm=gs.fromJson(jsonBody,SubmitModel.class);
            db.open();
            User u=db.getUserByToken(accessToken);
            if (u==null){
                db.close();
                ApiResult result=new ApiResult("403","Invalid access token",null);
                ServletHelper.writeResponse(result,response);
            }else{
                if (sm.liked_users!=null){
                    for (int a=0;a<sm.liked_users.size();a++){
                        db.removeLike(u.id,sm.liked_users.get(a));
                        db.saveLike(u.id,sm.liked_users.get(a));
                    }
                }
                if (sm.disliked_users!=null){
                    for (int a=0;a<sm.disliked_users.size();a++){
                        db.removeDislike(u.id,sm.disliked_users.get(a));
                        db.saveDislike(u.id,sm.disliked_users.get(a));
                    }
                }
                if (sm.matched_users!=null){
                    UserMatch um=new UserMatch();
                    um.user=new User();
                    for (int a=0;a<sm.matched_users.size();a++){
                        db.removeMatch(u.id,sm.matched_users.get(a).user_id);
                        um.user.id=sm.matched_users.get(a).user_id;
                        um.match_viewed=sm.matched_users.get(a).match_viewed;
                        um.match_announced=sm.matched_users.get(a).match_announced;
                        db.saveMatch(u.id,um);
                        
                        //now save another
                        db.removeMatch(sm.matched_users.get(a).user_id,u.id);
                        um.user.id=u.id;
                        um.match_viewed=sm.matched_users.get(a).match_viewed;
                        um.match_announced=sm.matched_users.get(a).match_announced;
                        db.saveMatch(sm.matched_users.get(a).user_id,um);
                    }
                }
                ApiResult result=new ApiResult("200",null,null);
                ServletHelper.writeResponse(result, response);
            }
            db.close();
        }catch(Exception ex){
            db.close();
            ApiResult result=new ApiResult("400",ex.getMessage(),null);
            ServletHelper.writeResponse(result,response);
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request,response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processPostRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Login controller";
    }// </editor-fold>
}
