package com.wugui.dataxweb.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.datax.common.log.EtlJobFileAppender;
import com.alibaba.datax.common.log.EtlJobLogger;
import com.alibaba.datax.common.log.LogResult;
import com.alibaba.datax.common.util.ProcessUtil;
import com.alibaba.datax.core.Engine;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wugui.dataxweb.dto.RunJobDto;
import com.wugui.dataxweb.entity.JobLog;
import com.wugui.dataxweb.service.IDataxJobService;
import com.wugui.dataxweb.service.IJobLogService;
import com.wugui.dataxweb.thread.ExecDataXOutputThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.*;

/**
 * @program: datax-all
 * @author: huzekang
 * @create: 2019-06-17 11:26
 **/
@Slf4j
@Service
public class IDataxJobServiceImpl implements IDataxJobService {


    private static final Logger log = LoggerFactory.getLogger(IDataxJobServiceImpl.class);

    private ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("datax-job-%d").build();

    private ExecutorService jobPool = new ThreadPoolExecutor(5, 200, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
    private String logFilePath;
    /**
     * 日志文件保存目录
     */
    @Value("${app.etlLogDir}")
    private String etlLogDir;

    @Autowired
    private IJobLogService jobLogService;


    @Override
    public String startJobByJsonStr(String jobJson, Long jobConfigId) {
        jobPool.submit(() -> {
            final String tmpFilePath = "jobTmp-" + System.currentTimeMillis() + ".json";
            // 根据json写入到临时本地文件
            try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
                writer.println(jobJson);
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                log.info("JSON 临时文件写入异常：{0}", e);
            }
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"python3", getDataXPyPath(), tmpFilePath});
                EtlJobFileAppender.appendLog(logFilePath, "\n\nJob:  " + jobConfigId + ",Datax运行进程Id:  " + ProcessUtil.getProcessId(p));
                ExecDataXOutputThread output = new ExecDataXOutputThread(p.getInputStream(), logFilePath, tmpFilePath);
                ExecDataXOutputThread error = new ExecDataXOutputThread(p.getErrorStream(), logFilePath, tmpFilePath);
                output.start();
                error.start();
            } catch (Exception e) {
                EtlJobFileAppender.appendLog(logFilePath, "\n\n经DataX智能分析,该任务最可能的错误原因是:\n" + "DATAX_HOME或者Job数据库配置信息有误");
                log.error("job 执行异常：{0}", e);
            }

        });

        return "success";
    }

    @Override
    public String startSyncJobByJsonStr(String jobJson, Long jobConfigId) {
        final String tmpFilePath = "jobTmp-" + System.currentTimeMillis() + ".json";
        // 根据json写入到临时本地文件
        try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
            writer.println(jobJson);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.info("JSON 临时文件写入异常：{0}", e);
        }
        String[] params = {"-mode", "standalone", "-jobid", "-1", "-job", tmpFilePath};
        // 使用 ClassLoader 获取资源路径
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("target/datax/datax");
        if (resource == null) {
            throw new IllegalArgumentException("文件夹未找到！");
        }
        System.setProperty("datax.home", resource.getPath());
        try {
            Engine.entry(params);
            //  删除临时文件
            FileUtil.del(new File(tmpFilePath));
        } catch (Throwable e) {
            log.error("", e);
        }
        return "success";
    }

    private String getDataXPyPath() {
        String dataxPyPath;
//        String dataXHome = System.getenv("DATAX_HOME");
        //todo lbl
        String dataXHome = "/Applications/myMac/zhongjian/code3/datax/DataX-SpringBoot-master/target/datax/datax/bin";
        if (StringUtils.isBlank(dataXHome)) {
            log.error("DATAX_HOME 环境变量为NULL");
            EtlJobFileAppender.appendLog(logFilePath, "DATAX_HOME 环境变量为NULL");
        }
        String osName = System.getProperty("os.name");
        dataXHome = osName.contains("Windows") ? (!dataXHome.endsWith("\\") ? dataXHome.concat("\\") : dataXHome) : (!dataXHome.endsWith("/") ? dataXHome.concat("/") : dataXHome);
        dataxPyPath = dataXHome + "datax.py";
        return dataxPyPath;
    }

    @Override
    public String startJobLog(RunJobDto runJobDto) {
        //取出 jobJson，并转为json对象
        JSONObject json = JSONObject.parseObject(runJobDto.getJobJson());
        //根据jobId和当前时间戳生成日志文件名
        String logFileName = runJobDto.getJobConfigId().toString().concat("_").concat(StrUtil.toString(System.currentTimeMillis()).concat(".log"));
        logFilePath = etlLogDir.concat(logFileName);
        //记录日志
        JobLog jobLog = new JobLog();
        jobLog.setJobId(runJobDto.getJobConfigId());
        jobLog.setLogFilePath(logFilePath);
        jobLogService.save(jobLog);
        //启动任务
        return startJobByJsonStr(JSON.toJSONString(json), runJobDto.getJobConfigId());
    }

    @Override
    public LogResult viewJogLog(Long id, int fromLineNum) {
        QueryWrapper<JobLog> queryWrapper = new QueryWrapper<>();
        //根据id获取最新的日志文件路径
        queryWrapper.lambda().eq(JobLog::getJobId, id).orderByDesc(JobLog::getCreateDate);
        List<JobLog> list = jobLogService.list(queryWrapper);
        //取最新的一条记录
        if (list.isEmpty()) {
            return new LogResult(1, 1, "没有找到对应的日志文件！", true);
        } else {
            //取出路径，读取文件
            return EtlJobLogger.readLog(list.get(0).getLogFilePath(), fromLineNum);
        }
    }
}
