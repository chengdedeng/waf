/*
 * Copyright 2018-present yangguo@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.yangguo.waf.config;

import info.yangguo.waf.dto.ResultDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("${error.path:/error}")
public class CustomErrorController implements ErrorController {

    @Value("${error.path:/error}")
    private String errorPath;

    @Override
    public String getErrorPath() {
        return this.errorPath;
    }

    @RequestMapping
    @ResponseBody
    public ResponseEntity<Object> error(HttpServletRequest request) {
        ResultDto resultDto = new ResultDto();
        HttpStatus status = getStatus(request);
        resultDto.setCode(status.value());
        resultDto.setValue(status.getReasonPhrase());
        return new ResponseEntity<Object>(resultDto, HttpStatus.OK);
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.valueOf(statusCode);
    }
}
