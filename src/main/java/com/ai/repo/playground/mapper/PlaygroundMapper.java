package com.ai.repo.playground.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;
import com.ai.repo.playground.entity.PlaygroundRows.*;

@Mapper
public interface PlaygroundMapper {
    @Select("SELECT id FROM playground_match_mutex WHERE id=1 FOR UPDATE") Integer matchMutex();
    @Select("SELECT * FROM playground_match_queue WHERE agent_id=#{id}") MatchEntry matchEntry(long id);
    @Select("SELECT * FROM playground_match_queue WHERE status IN ('WAITING','MATCHED') ORDER BY created_at LIMIT 100") List<MatchEntry> matchEntries();
    @Insert("INSERT INTO playground_match_queue(agent_id,user_id,permission_version,mode,brief_json,status,activity_id,retries,created_at,expires_at) VALUES(#{agentId},#{userId},#{permissionVersion},#{mode},#{briefJson},#{status},#{activityId},#{retries},#{createdAt},#{expiresAt}) ON DUPLICATE KEY UPDATE user_id=#{userId},permission_version=#{permissionVersion},mode=#{mode},brief_json=#{briefJson},status=#{status},activity_id=#{activityId},retries=#{retries},created_at=#{createdAt},expires_at=#{expiresAt}") int saveMatch(MatchEntry row);
    @Select("SELECT COUNT(*) FROM playground_match_queue WHERE agent_id=#{id} AND status='WAITING'") int waitingMatchCount(long id);
    @Select("SELECT COUNT(*) FROM playground_seats WHERE agent_id=#{id}") int seatCount(long id);
    @Select("SELECT COUNT(*) FROM playground_activities WHERE (host_agent_id=#{id} OR guest_agent_id=#{id}) AND status IN ('INVITED','WAITING','PLANNING')") int activeRoomCount(long id);
    @Select("SELECT COALESCE(MAX(attempts_used),0) FROM playground_daily_budgets WHERE agent_id=#{id} AND budget_date=#{date}") int dailyAttempts(@Param("id") long id,@Param("date") LocalDate date);
    @Select("SELECT COALESCE(MAX(games_started),0) FROM playground_daily_budgets WHERE agent_id=#{id} AND budget_date=#{date}") int dailyGames(@Param("id") long id,@Param("date") LocalDate date);
    @Select("SELECT COUNT(*) FROM playground_activities WHERE ((host_agent_id=#{one} AND guest_agent_id=#{two}) OR (host_agent_id=#{two} AND guest_agent_id=#{one})) AND created_at>#{since}") int recentPair(@Param("one") long one,@Param("two") long two,@Param("since") LocalDateTime since);

    @Select("SELECT id FROM agents WHERE id=#{id} FOR UPDATE") Long lockAgent(long id);
    @Select("SELECT * FROM playground_participations WHERE agent_id=#{id}") Participation participation(long id);
    @Select("SELECT * FROM playground_participations WHERE agent_id=#{id} FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE) Participation lockParticipation(long id);
    @Insert("INSERT INTO playground_participations(agent_id,user_id,version,enabled,max_decisions,max_attempts,max_daily_attempts,updated_at) VALUES(#{agentId},#{userId},#{version},#{enabled},#{maxDecisions},#{maxAttempts},#{maxDailyAttempts},#{updatedAt}) ON DUPLICATE KEY UPDATE version=#{version},enabled=#{enabled},max_decisions=#{maxDecisions},max_attempts=#{maxAttempts},max_daily_attempts=#{maxDailyAttempts},updated_at=#{updatedAt}")
    int saveParticipation(Participation row);
    @Update("UPDATE playground_seats SET permission_version=#{version} WHERE agent_id=#{agentId}")
    int rebindSeat(@Param("agentId") long agentId,@Param("version") long version);
    @Update("UPDATE playground_tasks SET permission_version=#{version},status='PENDING',lease_hash=NULL,lease_expires_at=NULL,attempt_id=NULL WHERE agent_id=#{agentId} AND status IN ('PENDING','LEASED')")
    int invalidateLeases(@Param("agentId") long agentId,@Param("version") long version);
    @Insert("INSERT INTO playground_activities(host_agent_id,guest_agent_id,horizon_months,status,state_json,next_sequence,expires_at,created_at,updated_at) VALUES(#{hostAgentId},#{guestAgentId},#{horizonMonths},#{status},#{stateJson},#{nextSequence},#{expiresAt},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertActivity(Activity row);
    @Select("SELECT COUNT(*) FROM playground_activities WHERE host_agent_id=#{agentId} AND status IN ('INVITED','WAITING','PLANNING')")
    int pendingHostCount(long agentId);
    @Select("SELECT a.* FROM playground_activities a JOIN agents h ON h.id=a.host_agent_id JOIN agents g ON g.id=a.guest_agent_id WHERE (h.user_id=#{userId} OR g.user_id=#{userId}) AND a.id<#{before} ORDER BY a.id DESC LIMIT 20")
    List<Activity> mine(@Param("userId") long userId,@Param("before") long before);
    @Select("SELECT * FROM playground_activities WHERE status='WAITING' AND expires_at>#{now} AND (host_agent_id=#{agentId} OR guest_agent_id=#{agentId}) ORDER BY id LIMIT 20")
    List<Activity> opportunities(@Param("agentId") long agentId,@Param("now") LocalDateTime now);
    @Select("SELECT * FROM playground_activities WHERE id=#{id}") Activity activity(long id);
    @Select("SELECT * FROM playground_activities WHERE id=#{id} FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE) Activity lockActivity(long id);
    @Select("SELECT * FROM playground_shares WHERE activity_id=#{activityId}") Share share(long activityId);
    @Select("SELECT * FROM playground_shares WHERE public_token=#{token} AND removed_at IS NULL") Share publishedShare(String token);
    @Insert("INSERT INTO playground_shares(activity_id,payload_json,public_token,created_at,removed_at,removed_by_user_id,removed_reason) VALUES(#{activityId},#{payloadJson},#{publicToken},#{createdAt},#{removedAt},#{removedByUserId},#{removedReason})") int insertShare(Share row);
    @Update("UPDATE playground_shares SET payload_json=#{payloadJson},public_token=#{publicToken} WHERE activity_id=#{activityId}") int saveShare(Share row);
    @Update("UPDATE playground_shares SET removed_at=#{removedAt},removed_by_user_id=#{removedByUserId},removed_reason=#{removedReason} WHERE activity_id=#{activityId} AND removed_at IS NULL") int removeShare(Share row);
    @Update("UPDATE playground_activities SET status=#{status},state_json=#{stateJson},next_sequence=#{nextSequence},expires_at=#{expiresAt},updated_at=#{updatedAt} WHERE id=#{id}") int saveActivity(Activity row);
    @Select("SELECT * FROM playground_seats WHERE agent_id=#{id} FOR UPDATE") Seat seat(long id);
    @Insert("INSERT INTO playground_seats(agent_id,activity_id,permission_version,decisions_used,attempts_used) VALUES(#{agentId},#{activityId},#{permissionVersion},#{decisionsUsed},#{attemptsUsed})") int insertSeat(Seat row);
    @Update("UPDATE playground_seats SET decisions_used=#{decisionsUsed},attempts_used=#{attemptsUsed} WHERE agent_id=#{agentId} AND activity_id=#{activityId}") int saveSeat(Seat row);
    @Delete("DELETE FROM playground_seats WHERE activity_id=#{id}") int releaseSeats(long id);
    @Insert("INSERT INTO playground_tasks(activity_id,agent_id,permission_version,status,phase,expires_at,created_at) VALUES(#{activityId},#{agentId},#{permissionVersion},#{status},#{phase},#{expiresAt},#{createdAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertTask(Task row);
    @Select("SELECT * FROM playground_tasks WHERE id=#{id}")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE) Task task(long id);
    @Select("SELECT * FROM playground_tasks WHERE id=#{id} FOR UPDATE")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE) Task lockTask(long id);
    @Update("UPDATE playground_tasks SET status=#{status},lease_hash=#{leaseHash},lease_expires_at=#{leaseExpiresAt},attempt_id=#{attemptId} WHERE id=#{id}") int saveTask(Task row);
    @Select("SELECT t.* FROM playground_tasks t JOIN playground_participations p ON p.agent_id=t.agent_id JOIN playground_seats s ON s.agent_id=t.agent_id AND s.activity_id=t.activity_id JOIN playground_activities a ON a.id=t.activity_id WHERE t.agent_id=#{agentId} AND t.status IN ('PENDING','LEASED') AND p.enabled=TRUE AND t.permission_version=p.version AND s.permission_version=p.version AND t.expires_at>#{now} AND a.status='PLANNING' ORDER BY t.id LIMIT 20")
    List<Task> tasks(@Param("agentId") long agentId, @Param("now") LocalDateTime now);
    @Update("UPDATE playground_tasks SET status='CANCELLED',lease_hash=NULL,lease_expires_at=NULL WHERE activity_id=#{id} AND status IN ('PENDING','LEASED')") int cancelTasks(long id);
    @Insert("INSERT INTO playground_attempts(task_id,agent_id,request_key,lease_hash,status,created_at) VALUES(#{taskId},#{agentId},#{requestKey},#{leaseHash},#{status},#{createdAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertAttempt(Attempt row);
    @Select("SELECT * FROM playground_attempts WHERE agent_id=#{agentId} AND request_key=#{key} FOR UPDATE")
    Attempt attemptByKey(@Param("agentId") long agentId,@Param("key") String key);
    @Select("SELECT * FROM playground_attempts WHERE id=#{id} FOR UPDATE") Attempt attempt(long id);
    @Update("UPDATE playground_attempts SET status=#{status},failure_reason=#{failureReason} WHERE id=#{id}") int saveAttempt(Attempt row);
    @Insert("INSERT IGNORE INTO playground_daily_budgets(agent_id,budget_date,attempts_used) VALUES(#{agentId},#{date},0)")
    int ensureDaily(@Param("agentId") long agentId, @Param("date") LocalDate date);
    @Select("SELECT attempts_used FROM playground_daily_budgets WHERE agent_id=#{agentId} AND budget_date=#{date} FOR UPDATE")
    int lockDaily(@Param("agentId") long agentId, @Param("date") LocalDate date);
    @Select("SELECT games_started FROM playground_daily_budgets WHERE agent_id=#{agentId} AND budget_date=#{date} FOR UPDATE")
    int lockDailyGames(@Param("agentId") long agentId,@Param("date") LocalDate date);
    @Update("UPDATE playground_daily_budgets SET games_started=games_started+1 WHERE agent_id=#{agentId} AND budget_date=#{date}")
    int incrementDailyGames(@Param("agentId") long agentId,@Param("date") LocalDate date);
    @Update("UPDATE playground_daily_budgets SET attempts_used=attempts_used+1 WHERE agent_id=#{agentId} AND budget_date=#{date}")
    int incrementDaily(@Param("agentId") long agentId, @Param("date") LocalDate date);
    @Select("SELECT * FROM playground_actions WHERE agent_id=#{agentId} AND request_key=#{key} FOR UPDATE")
    Receipt receipt(@Param("agentId") long agentId, @Param("key") String key);
    @Insert("INSERT INTO playground_actions(agent_id,task_id,request_key,request_hash,response_json,created_at) VALUES(#{agentId},#{taskId},#{requestKey},#{requestHash},#{responseJson},#{createdAt})") int insertReceipt(Receipt row);
    @Insert("INSERT INTO playground_events(activity_id,sequence,event_json) VALUES(#{activityId},#{sequence},#{json})")
    int insertEvent(@Param("activityId") long activityId, @Param("sequence") long sequence, @Param("json") String json);
    @Select("SELECT event_json FROM playground_events WHERE activity_id=#{activityId} AND sequence>#{after} ORDER BY sequence LIMIT 50")
    List<String> events(@Param("activityId") long activityId, @Param("after") long after);
    @Select("SELECT COUNT(*) FROM playground_tasks WHERE activity_id=#{activityId} AND status IN ('PENDING','LEASED') AND expires_at<=#{now}")
    int expiredTaskCount(@Param("activityId") long activityId,@Param("now") LocalDateTime now);
    @Select("SELECT * FROM playground_activities WHERE status IN ('INVITED','WAITING','PLANNING') AND (expires_at<=#{now} OR EXISTS (SELECT 1 FROM playground_tasks t WHERE t.activity_id=playground_activities.id AND t.status IN ('PENDING','LEASED') AND t.expires_at<=#{now})) ORDER BY id LIMIT 100")
    List<Activity> expired(LocalDateTime now);
}
