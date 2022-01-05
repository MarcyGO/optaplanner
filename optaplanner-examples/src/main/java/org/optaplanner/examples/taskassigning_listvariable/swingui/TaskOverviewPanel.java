/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.taskassigning_listvariable.swingui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.optaplanner.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
import org.optaplanner.core.impl.heuristic.selector.move.generic.list.ListChangeMove;
import org.optaplanner.core.impl.heuristic.selector.move.generic.list.ListUnassignMove;
import org.optaplanner.examples.common.swingui.SolutionPanel;
import org.optaplanner.examples.common.swingui.components.LabeledComboBoxRenderer;
import org.optaplanner.examples.taskassigning_listvariable.domain.Employee;
import org.optaplanner.examples.taskassigning_listvariable.domain.Skill;
import org.optaplanner.examples.taskassigning_listvariable.domain.Task;
import org.optaplanner.examples.taskassigning_listvariable.domain.TaskAssigningSolution;
import org.optaplanner.swing.impl.SwingUtils;
import org.optaplanner.swing.impl.TangoColorFactory;

public class TaskOverviewPanel extends JPanel implements Scrollable {

    public static final int HEADER_ROW_HEIGHT = 50;
    public static final int HEADER_COLUMN_WIDTH = 150;
    public static final int ROW_HEIGHT = 50;
    public static final int TIME_COLUMN_WIDTH = 60;

    private final TaskAssigningPanel taskAssigningPanel;
    private final ImageIcon[] affinityIcons;
    private final ImageIcon[] priorityIcons;

    private TangoColorFactory skillColorFactory;

    private int consumedDuration = 0;

    public TaskOverviewPanel(TaskAssigningPanel taskAssigningPanel) {
        this.taskAssigningPanel = taskAssigningPanel;
        affinityIcons = new ImageIcon[] {
                new ImageIcon(getClass().getResource("affinityNone.png")),
                new ImageIcon(getClass().getResource("affinityLow.png")),
                new ImageIcon(getClass().getResource("affinityMedium.png")),
                new ImageIcon(getClass().getResource("affinityHigh.png"))
        };
        priorityIcons = new ImageIcon[] {
                new ImageIcon(getClass().getResource("priorityMinor.png")),
                new ImageIcon(getClass().getResource("priorityMajor.png")),
                new ImageIcon(getClass().getResource("priorityCritical.png"))
        };
        setLayout(null);
        setMinimumSize(new Dimension(HEADER_COLUMN_WIDTH * 2, ROW_HEIGHT * 8));
    }

    public void resetPanel(TaskAssigningSolution taskAssigningSolution) {
        removeAll();
        skillColorFactory = new TangoColorFactory();
        List<Employee> employeeList = taskAssigningSolution.getEmployeeList();
        List<Task> unassignedTaskList = new ArrayList<>(taskAssigningSolution.getTaskList());

        int rowIndex = 0;
        for (Employee employee : employeeList) {
            JLabel employeeLabel = new JLabel(employee.getLabel(), new TaskOrEmployeeIcon(employee), SwingConstants.LEFT);
            employeeLabel.setOpaque(true);
            employeeLabel.setToolTipText(employee.getToolText());
            employeeLabel.setLocation(0, HEADER_ROW_HEIGHT + rowIndex * ROW_HEIGHT);
            employeeLabel.setSize(HEADER_COLUMN_WIDTH, ROW_HEIGHT);
            employeeLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            add(employeeLabel);

            for (Task task : employee.getTasks()) {
                add(createTaskButton(task, rowIndex));
                unassignedTaskList.remove(task);
            }
            rowIndex++;
        }

        for (Task task : unassignedTaskList) {
            add(createTaskButton(task, rowIndex));
            rowIndex++;
        }

        int maxUnassignedTaskDuration = unassignedTaskList.stream().mapToInt(Task::getDuration).max().orElse(0);
        int maxEmployeeEndTime = employeeList.stream().mapToInt(Employee::getEndTime).max().orElse(0);

        int taskTableWidth = Math.max(maxEmployeeEndTime, maxUnassignedTaskDuration + consumedDuration);

        for (int x = 0; x < taskTableWidth; x += TIME_COLUMN_WIDTH) {
            // Use 10 hours per day
            int minutes = x % (10 * 60);
            // Start at 8:00
            int hours = 8 + (minutes / 60);
            minutes %= 60;
            JLabel timeLabel = new JLabel((hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes);
            timeLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            timeLabel.setLocation(x + HEADER_COLUMN_WIDTH, 0);
            timeLabel.setSize(TIME_COLUMN_WIDTH, ROW_HEIGHT);
            add(timeLabel);
        }
        if (taskTableWidth % TIME_COLUMN_WIDTH != 0) {
            taskTableWidth += TIME_COLUMN_WIDTH - (taskTableWidth % TIME_COLUMN_WIDTH);
        }

        Dimension size = new Dimension(taskTableWidth + HEADER_COLUMN_WIDTH, HEADER_ROW_HEIGHT + rowIndex * ROW_HEIGHT);
        setSize(size);
        setPreferredSize(size);
        repaint();
    }

    public void setConsumedDuration(int consumedDuration) {
        this.consumedDuration = consumedDuration;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(TangoColorFactory.ALUMINIUM_2);
        int lineX = HEADER_COLUMN_WIDTH + consumedDuration;
        g.fillRect(HEADER_COLUMN_WIDTH, 0, lineX, getHeight());
        g.setColor(Color.WHITE);
        g.fillRect(lineX, 0, getWidth(), getHeight());
    }

    private JButton createTaskButton(Task task, int rowIndex) {
        JButton taskButton = SwingUtils.makeSmallButton(new JButton(new TaskAction(task)));
        taskButton.setBackground(task.isPinned() ? TangoColorFactory.ALUMINIUM_3 : TangoColorFactory.ALUMINIUM_1);
        taskButton.setHorizontalTextPosition(SwingConstants.CENTER);
        taskButton.setVerticalTextPosition(SwingConstants.TOP);
        taskButton.setSize(task.getDuration(), ROW_HEIGHT);
        int x = HEADER_COLUMN_WIDTH + (task.getEmployee() == null ? task.getReadyTime() : task.getStartTime());
        int y = HEADER_ROW_HEIGHT + rowIndex * ROW_HEIGHT;
        taskButton.setLocation(x, y);
        return taskButton;
    }

    private class TaskAction extends AbstractAction {

        private final Task task;

        public TaskAction(Task task) {
            super(task.getCode(), new TaskOrEmployeeIcon(task));
            this.task = task;
            // Tooltip
            putValue(SHORT_DESCRIPTION, task.getToolText());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox<Integer> indexListField = new JComboBox<>();
            List<Employee> employees = taskAssigningPanel.getSolution().getEmployeeList();
            JComboBox<Employee> employeeListField = new JComboBox<>(employees.toArray(new Employee[0]));
            employeeListField.addItemListener(itemEvent -> {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    Employee selectedEmployee = (Employee) itemEvent.getItem();
                    int availableIndexes = selectedEmployee.getTasks() == null ? 0 : selectedEmployee.getTasks().size();
                    if (selectedEmployee == task.getEmployee()) {
                        availableIndexes--;
                    }
                    Integer[] data = new Integer[availableIndexes + 1];
                    IntStream.rangeClosed(0, availableIndexes).boxed().collect(Collectors.toList()).toArray(data);
                    indexListField.setModel(new DefaultComboBoxModel<>(data));
                }
            });
            LabeledComboBoxRenderer.applyToComboBox(employeeListField);
            // Without selecting null first, the next select wouldn't call the item listener if the selected employee
            // is the first on the list (and the index combo wouldn't be populated).
            employeeListField.setSelectedItem(null);
            if (task.getEmployee() == null) {
                employeeListField.setSelectedIndex(0);
            } else {
                employeeListField.setSelectedItem(task.getEmployee());
            }

            JCheckBox unassignCheckBox = new JCheckBox("Or unassign.");
            unassignCheckBox.addActionListener(checkBoxEvent -> {
                employeeListField.setEnabled(!unassignCheckBox.isSelected());
                indexListField.setEnabled(!unassignCheckBox.isSelected());
            });
            unassignCheckBox.setVisible(task.getEmployee() != null);

            JPanel listFieldsPanel = new JPanel(new GridLayout(4, 1));
            listFieldsPanel.add(new JLabel("Select employee and index:"));
            listFieldsPanel.add(employeeListField);
            listFieldsPanel.add(indexListField);
            listFieldsPanel.add(unassignCheckBox);
            int result = JOptionPane.showConfirmDialog(TaskOverviewPanel.this.getRootPane(),
                    listFieldsPanel, "Move " + task.getCode(),
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                Move<TaskAssigningSolution> move;
                if (unassignCheckBox.isSelected()) {
                    move = new ListUnassignMove<>(
                            getTaskListVariableDescriptor(task.getEmployee()),
                            task.getEmployee(),
                            task.getIndex());
                } else {
                    Employee selectedEmployee = (Employee) employeeListField.getSelectedItem();
                    Integer selectedIndex = (Integer) indexListField.getSelectedItem();
                    if (task.getEmployee() == null) {
                        move = new ListAssignMove<>(
                                getTaskListVariableDescriptor(selectedEmployee),
                                task,
                                selectedEmployee,
                                selectedIndex);
                    } else {
                        move = new ListChangeMove<>(
                                getTaskListVariableDescriptor(selectedEmployee),
                                task.getEmployee(),
                                task.getIndex(),
                                selectedEmployee,
                                selectedIndex);
                    }
                }
                taskAssigningPanel.getSolutionBusiness().doMove(move);
                taskAssigningPanel.getSolverAndPersistenceFrame().resetScreen();
            }
        }

    }

    private ListVariableDescriptor<TaskAssigningSolution> getTaskListVariableDescriptor(Employee employee) {
        return (ListVariableDescriptor<TaskAssigningSolution>) taskAssigningPanel.getSolutionBusiness()
                .findVariableDescriptor(employee, "tasks");
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return SolutionPanel.PREFERRED_SCROLLABLE_VIEWPORT_SIZE;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return (getParent().getWidth() > getPreferredSize().width);
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            return (getParent().getHeight() > getPreferredSize().height);
        }
        return false;
    }

    private class TaskOrEmployeeIcon implements Icon {

        private static final int SKILL_ICON_WIDTH = 8;
        private static final int SKILL_ICON_HEIGHT = 16;

        private final ImageIcon priorityIcon;
        private final List<Color> skillColorList;
        private final ImageIcon affinityIcon;

        private TaskOrEmployeeIcon(Task task) {
            priorityIcon = priorityIcons[task.getPriority().ordinal()];
            skillColorList = task.getTaskType().getRequiredSkillList().stream()
                    .map(skillColorFactory::pickColor)
                    .collect(Collectors.toList());
            affinityIcon = affinityIcons[task.getAffinity().ordinal()];
        }

        private TaskOrEmployeeIcon(Employee employee) {
            priorityIcon = null;
            skillColorList = employee.getSkillSet().stream()
                    .sorted(Comparator.comparing(Skill::getName))
                    .map(skillColorFactory::pickColor)
                    .collect(Collectors.toList());
            affinityIcon = null;
        }

        @Override
        public int getIconWidth() {
            int width = 0;
            if (priorityIcon != null) {
                width += priorityIcon.getIconWidth();
            }
            width += skillColorList.size() * SKILL_ICON_WIDTH;
            if (affinityIcon != null) {
                width += affinityIcon.getIconWidth();
            }
            return width;
        }

        @Override
        public int getIconHeight() {
            int height = SKILL_ICON_HEIGHT;
            if (priorityIcon != null && priorityIcon.getIconHeight() > height) {
                height = priorityIcon.getIconHeight();
            }
            if (affinityIcon != null && affinityIcon.getIconHeight() > height) {
                height = affinityIcon.getIconHeight();
            }
            return height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            int innerX = x;
            if (priorityIcon != null) {
                priorityIcon.paintIcon(c, g, innerX, y);
                innerX += priorityIcon.getIconWidth();
            }
            for (Color skillColor : skillColorList) {
                g.setColor(skillColor);
                g.fillRect(innerX + 1, y + 1, SKILL_ICON_WIDTH - 2, SKILL_ICON_HEIGHT - 2);
                g.setColor(TangoColorFactory.ALUMINIUM_5);
                g.drawRect(innerX + 1, y + 1, SKILL_ICON_WIDTH - 2, SKILL_ICON_HEIGHT - 2);
                innerX += SKILL_ICON_WIDTH;
            }
            if (affinityIcon != null) {
                affinityIcon.paintIcon(c, g, innerX, y);
                innerX += affinityIcon.getIconWidth();
            }
        }

    }

}
