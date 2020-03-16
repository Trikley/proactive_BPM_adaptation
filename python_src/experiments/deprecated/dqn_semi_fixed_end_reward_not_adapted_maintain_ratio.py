import gym
import tensorflow as tf

from stable_baselines.deepq.policies import MlpPolicy
from stable_baselines.common.vec_env import DummyVecEnv
from stable_baselines import DQN
import os

inner_env = None


def run(learning_steps=4300, verbose=0, gamma=0.99, learning_rate=5e-4, tensorboard_log="tensorboard"):
    global inner_env
    inner_env = gym.make('gym_threshold:semi-fixed-end-not-adapted-maintain-v0')
    env = DummyVecEnv([lambda: inner_env])

    model = DQN(MlpPolicy, env, prioritized_replay=True, verbose=verbose, learning_rate=learning_rate, gamma=gamma,
                tensorboard_log=tensorboard_log)
    model.learn(total_timesteps=learning_steps, tb_log_name=os.path.basename(__file__).rstrip(".py"),
                callback=tensorboard_callback)

    env.close()


def tensorboard_callback(locals_, globals_):
    global inner_env
    self_ = locals_['self']
    if inner_env.summary_writer is None:
        inner_env.summary_writer = locals_['writer']

    return True


if __name__ == "__main__":
    run(2000000)
