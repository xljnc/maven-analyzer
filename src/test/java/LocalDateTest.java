import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

/**
 * @author 一贫
 * @date 2021/6/11
 */
@Slf4j
public class LocalDateTest {
    public static void main(String[] args) {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now();
        log.info("扫描完成。启动时间:{},结束时间:{}", startTime.toString(), endTime.toString());
//        Period p = Period.between(startTime, endTime);
//        log.info("花费时间:{}天{}时{}分{}秒", p.get(ChronoUnit.DAYS), p.get(ChronoUnit.HOURS), p.get(ChronoUnit.MINUTES), p.get(ChronoUnit.SECONDS));
    }
}
