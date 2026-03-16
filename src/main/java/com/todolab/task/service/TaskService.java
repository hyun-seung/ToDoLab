package com.todolab.task.service;

import com.todolab.Constant;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskCategoryGroupResponse;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskTxService taskTxService;
    private final TaskRepository taskRepository;

    public TaskResponse create(TaskRequest req) {
        Task task = Task.builder()
                .title(req.title())
                .description(req.description())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .allDay(req.allDay())
                .category(req.category())
                .build();

        Task saved = taskRepository.save(task);
        return TaskResponse.from(saved);
    }

    public TaskResponse getTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        return TaskResponse.from(task);
    }

    public List<TaskCategoryGroupResponse> getTasks(TaskQueryRequest request) {
        return groupByCategory(findTasks(request));
    }

    public List<TaskCategoryGroupResponse> getUnscheduledTasks() {
        return groupByCategory(findUnscheduledTasks());
    }

    public TaskResponse update(Long id, TaskRequest taskRequest) {
        Task updated = taskTxService.updateTx(id, taskRequest);
        return TaskResponse.from(updated);
    }

    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }

    private List<TaskResponse> findTasks(TaskQueryRequest request) {
        final TaskQueryType type = request.getType();
        final String strDate = request.getDate();

        DateRange range = type.calculate(strDate);

        return taskRepository.findByDateRange(range.getStart(), range.getEnd())
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    private List<TaskResponse> findUnscheduledTasks() {
        return taskRepository.findUnscheduledTask().stream()
                .map(TaskResponse::from)
                .toList();
    }

    private List<TaskCategoryGroupResponse> groupByCategory(List<TaskResponse> tasks) {
        Map<String, List<TaskResponse>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(task -> toCategoryLabel(task.category())));

        return grouped.entrySet().stream()
                .sorted((a, b) -> {
                    boolean aUncategorized = a.getKey().equals(Constant.UNCATEGORIZED);
                    boolean bUncategorized = b.getKey().equals(Constant.UNCATEGORIZED);

                    if (aUncategorized && !bUncategorized) {
                        return 1;
                    }
                    if (!aUncategorized && bUncategorized) {
                        return -1;
                    }
                    return a.getKey().compareTo(b.getKey());
                })
                .map(entry -> new TaskCategoryGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String toCategoryLabel(String category) {
        if (category == null || category.isBlank()) {
            return Constant.UNCATEGORIZED;
        }
        return category;
    }
}
