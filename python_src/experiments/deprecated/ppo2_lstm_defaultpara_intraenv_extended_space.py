import gym
import tensorflow as tf

from stable_baselines.common.policies import MlpLstmPolicy
from stable_baselines.common.vec_env import DummyVecEnv
from stable_baselines import PPO2
import os

inner_env = None


def run(learning_steps=4300):
    global inner_env
    inner_env = gym.make('gym_threshold:threshold-intra_process-extended-space-v0')
    env = DummyVecEnv([lambda: inner_env])

    model = PPO2(MlpLstmPolicy, env, verbose=1, n_steps=32, nminibatches=1,
                 tensorboard_log="tensorboard")
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
