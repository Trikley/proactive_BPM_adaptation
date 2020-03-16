import gym
import tensorflow as tf

from stable_baselines.common.policies import MlpPolicy
from stable_baselines.common.vec_env import DummyVecEnv
from stable_baselines import PPO2
import os

inner_env = None


def run(learning_steps=4300, verbose=0, n_steps=128, nminibatches=4, gamma=0.99, learning_rate=2.5e-4, ent_coef=0.01, tensorboard_log="tensorboard"):
    global inner_env
    inner_env = gym.make('gym_threshold:extended-state-semi-fixed-end-not-adapted-v0')
    env = DummyVecEnv([lambda: inner_env])

    model = PPO2(MlpPolicy, env, verbose=verbose, n_steps=n_steps, nminibatches=nminibatches, gamma=gamma,
                 ent_coef=ent_coef, learning_rate=learning_rate,
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
