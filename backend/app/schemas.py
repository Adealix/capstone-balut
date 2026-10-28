"""Explicit request fields keep validation errors near the form that caused them."""

from typing import Literal
from pydantic import BaseModel, Field


class Login(BaseModel):
    username: str = Field(min_length=1, max_length=60)
    password: str = Field(min_length=1, max_length=200)


class Account(Login):
    name: str = Field(min_length=1, max_length=100)
    role: Literal["Owner", "Operator"] = "Operator"


class BatchInput(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    source: str = Field(default="", max_length=200)
    quantity: int = Field(ge=1, le=30)
    started: str
    notes: str = Field(default="", max_length=2000)
    location: Literal["C1-1", "C1-2"]
    tray_id: str = ""


class BatchEdit(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    source: str = Field(default="", max_length=200)
    notes: str = Field(default="", max_length=2000)
    started: str
    reason: str = Field(default="", max_length=500)
    version: int


class Transfer(BaseModel):
    quantity: int = Field(ge=1, le=30)
    destination: Literal["C1-1", "C1-2", "C2", "C3-1", "C3-2", "C3-3"]
    row_name: Literal["", "A", "B", "C", "D", "E"] = ""
    purpose: Literal[
        "Unassigned",
        "Penoy processing",
        "Balut production",
        "Duckling hatching",
        "Recheck",
    ] = "Unassigned"
    candidate: Literal[
        "Unreviewed", "Developing", "Non-developing", "Abnormal", "Recheck"
    ] = "Unreviewed"
    target: str = ""
    version: int


class Complete(BaseModel):
    quantity: int = Field(ge=1, le=30)
    outcome: Literal["Harvested", "Hatched", "Removed", "Discarded"]
    version: int


class AlertAction(BaseModel):
    action: Literal["acknowledge", "snooze", "resolve"]
    until: str = ""


class Command(BaseModel):
    command: Literal[
        "Pause turning", "Resume turning", "Candling light on", "Candling light off"
    ]
    location: Literal["C1-1", "C1-2", "C2", "C3-1", "C3-2", "C3-3"]


class Profile(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    current_password: str = ""
    new_password: str = Field(default="", max_length=200)


class Farm(BaseModel):
    name: str = Field(min_length=1, max_length=100)


class UserState(BaseModel):
    active: bool


class TrayInput(BaseModel):
    id: str = Field(min_length=1, max_length=30, pattern=r"^[A-Za-z0-9_-]+$")


class TrayState(BaseModel):
    state: Literal["Available", "Cleaning", "Unavailable"]
