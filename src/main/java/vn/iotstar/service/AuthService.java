package vn.iotstar.service;

import vn.iotstar.model.ApiResponse;
import vn.iotstar.model.LoginModel;
import vn.iotstar.model.NguoiDungModel;

public interface AuthService {
    ApiResponse<String> authenticateUser(LoginModel loginModel);
    ApiResponse<String> registerUser(NguoiDungModel signUpModel);
    boolean validateUser(String email, String password);
}