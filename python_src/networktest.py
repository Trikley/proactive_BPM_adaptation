import numpy as np
import os
import socket


class MockEnvironment(object):
    def __init__(self):
        self.total_steps = 0
        self.episode_count = 0
        self.adapted_count = 0

        # learning parameter
        self.reward = 0
        self.rewards_per_episode = []
        self.action_value = 0
        self.case_id = -1
        self.actual_duration = -1
        self.predicted_duration = 0
        self.planned_duration = 0
        self.position = 0
        self.cost = 0
        self.process_length = 0
        self.done = 0
        self.adapted = 0
        self.reliability = 0

        self.true = False

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        # now connect to the web server on port 80 - the normal http port
        self.socket.connect(("localhost", 1337))

        self.net = self.socket.makefile("rw", 65536, newline=os.linesep)

        self.state = None

    def step(self, action=None):
        if action is None:
            action = 1

        self.send_action(int(action))
        self.action_value = action

        self.receive_reward_and_state()

        info = {}
        self.state = np.array([self.planned_duration, self.reliability])

        return self.state, self.reward, self.done, info

    def send_action(self, action):
        print("sending action...")
        self.net.writelines([str(action) + os.linesep])
        self.net.flush()
        print("action sent: " + str(action))

    def receive_reward_and_state(self):
        print("receiving reward parameters...")
        adapted = self.net.readline()
        adapted = adapted.strip()
        adapted = True if adapted == 'true' else False
        cost = self.net.readline()
        cost = cost.strip()
        cost = float(cost)
        done = self.net.readline()
        done = done.strip()
        done = True if done == 'true' else False
        if done:
            true = self.net.readline()
            true = true.strip()
            true = True if true == 'true' else False
            self.true = true

        case_id = self.net.readline()
        case_id = case_id.strip()
        case_id = float(case_id)
        actual_duration = self.net.readline()
        actual_duration = actual_duration.strip()
        actual_duration = float(actual_duration)
        predicted_duration = self.net.readline()
        predicted_duration = predicted_duration.strip()
        predicted_duration = float(predicted_duration)
        planned_duration = self.net.readline()
        planned_duration = planned_duration.strip()
        planned_duration = float(planned_duration)
        reliability = self.net.readline()
        reliability = reliability.strip()
        reliability = float(reliability)
        position = self.net.readline()
        position = position.strip()
        position = float(position)
        process_length = self.net.readline()
        process_length = process_length.strip()
        process_length = float(process_length)

        # compute the reward
        reward = -(50. / process_length)

        self.adapted = adapted
        self.cost = cost
        self.done = done
        self.case_id = case_id
        self.actual_duration = actual_duration
        self.predicted_duration = predicted_duration
        self.planned_duration = planned_duration
        self.reliability = reliability
        self.reward = reward

        if position != 0.:
            self.position = position
            self.process_length = process_length

        print("Received:")
        print([reward, done, predicted_duration, planned_duration, reliability, position, process_length, cost, adapted])
        print("")
        return reward, done, predicted_duration, planned_duration, reliability, position, process_length, cost, adapted

    def reset(self):
        self.send_action(-1)
        self.receive_reward_and_state()

        if self.process_length == 0:
            relative_position = 0
        else:
            relative_position = (self.position / self.process_length)
        self.state = np.array([relative_position, self.reliability])

        return self.state

    def close(self):
        print("Closing file and socket...")
        self.net.close()
        self.socket.close()
        print("Closed!")


env = MockEnvironment()

env.reset()

env.step(1)
env.step(1)
env.step(1)
env.step(1)
env.step(1)
env.step(1)
env.reset()

env.step(1)
env.step(2)
env.reset()

env.close()
